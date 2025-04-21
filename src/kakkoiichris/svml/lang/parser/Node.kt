/*   ______  ____   ____  ____    ____  _____
 * .' ____ \|_  _| |_  _||_   \  /   _||_   _|
 * | (___ \_| \ \   / /    |   \/   |    | |
 *  _.____`.   \ \ / /     | |\  /| |    | |   _
 * | \____) |   \ ' /     _| |_\/_| |_  _| |__/ |
 *  \______.'    \_/     |_____||_____||________|
 *
 *         Stack Virtual Machine Language
 *     Copyright (C) 2024 Christian Alexander
 */
package kakkoiichris.svml.lang.parser

import kakkoiichris.svml.lang.compiler.Bytecode
import kakkoiichris.svml.lang.lexer.Context
import kakkoiichris.svml.lang.lexer.TokenType

typealias Nodes = MutableList<Node>

sealed class Node(val context: Context) {
    var dataType: DataType = DataType.Primitive.VOID

    fun getArrayType() = DataType.asArray(dataType, context.source)

    abstract fun <X> accept(visitor: Visitor<X>): X

    interface Visitor<X> {
        fun visit(node: Node) =
            node.accept(this)

        fun visitProgram(node: Program): X

        fun visitDeclare(node: Declare): X

        fun visitIf(node: If): X

        fun visitWhile(node: While): X

        fun visitDo(node: Do): X

        fun visitFor(node: For): X

        fun visitBreak(node: Break): X

        fun visitContinue(node: Continue): X

        fun visitFunction(node: Function): X

        fun visitReturn(node: Return): X

        fun visitExpression(node: Expression): X

        fun visitValue(node: Value): X

        fun visitString(node: String): X

        fun visitName(node: Name): X

        fun visitArray(node: Array): X

        fun visitUnary(node: Unary): X

        fun visitSize(node: Size): X

        fun visitIndexSize(node: IndexSize): X

        fun visitBinary(node: Binary): X

        fun visitLogical(node: Logical): X

        fun visitAssign(node: Assign): X

        fun visitInvoke(node: Invoke): X

        fun visitGetIndex(node: GetIndex): X

        fun visitSetIndex(node: SetIndex): X
    }

    class Program(
        context: Context,
        val declarations: List<Declare>,
        val functions: List<Function>
    ) : Node(context) {
        lateinit var mainReturn: Return

        var offset = -1

        val subNodes = declarations + functions

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitProgram(this)
    }

    class Declare(
        context: Context,
        val isConstant: Boolean,
        val isMutable: Boolean,
        val name: Name,
        var type: Type?,
        val assigned: Node?
    ) : Node(context) {
        var id = -1
        var address = -1

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitDeclare(this)
    }

    class If(context: Context, val branches: List<Branch>) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitIf(this)

        fun toExpr(): If {
            val newBranches = mutableListOf<Branch>()

            for ((location, condition, body) in branches) {
                val newBody = body.toMutableList()

                val last = newBody.removeLast()

                if (last !is Expression) error("Last statement of if expression must be an expression @ ${last.context}!")

                newBody += last.node

                newBranches += Branch(location, condition, newBody)
            }

            return If(context, newBranches)
        }

        data class Branch(val context: Context, val condition: Node?, val body: Nodes)
    }

    class While(context: Context, val condition: Node, val label: Name?, val body: Nodes) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitWhile(this)
    }

    class Do(context: Context, val label: Name?, val body: Nodes, val condition: Node) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitDo(this)
    }

    class For(
        context: Context,
        val init: Declare?,
        val condition: Node?,
        val increment: Node?,
        val label: Name?,
        val body: Nodes
    ) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitFor(this)
    }

    class Break(context: Context, val label: Name?) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitBreak(this)
    }

    class Continue(context: Context, val label: Name?) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitContinue(this)
    }

    class Function(
        context: Context,
        val name: Name,
        val params: List<Declare>,
        val type: Type,
        val isNative: Boolean,
        val body: Nodes
    ) : Node(context) {
        val signature = Signature(name, params.map { it.type!!.value })

        val id = signature.hashCode()

        var offset = -1

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitFunction(this)
    }

    class Return(context: Context, val value: Node?) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitReturn(this)
    }

    class Expression(context: Context, val node: Node) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitExpression(this)
    }

    class Value(context: Context, val value: TokenType.Value) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitValue(this)
    }

    class String(context: Context, val value: kotlin.String) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitString(this)
    }

    class Name(
        context: Context,
        val value: kotlin.String,
    ) : Node(context) {
        var id = -1
        var address = -1
        var isGlobal = false

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitName(this)
    }

    class Array(context: Context, val elements: Nodes) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitArray(this)
    }

    class Unary(context: Context, val operator: Operator, val operand: Node) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitUnary(this)

        enum class Operator(val symbol: TokenType.Symbol, val instruction: Bytecode.Instruction) {
            NEGATE(
                TokenType.Symbol.DASH,
                Bytecode.Instruction.NEG
            ),

            INVERT(
                TokenType.Symbol.EXCLAMATION,
                Bytecode.Instruction.NOT
            );

            companion object {
                operator fun get(symbol: TokenType) =
                    entries.first { it.symbol == symbol }
            }
        }
    }

    class Size(context: Context, val name: Name) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSize(this)
    }

    class IndexSize(context: Context, val name: Name, val indices: List<Node>) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitIndexSize(this)
    }

    class Binary(
        context: Context,
        val operator: Operator,
        val operandLeft: Node,
        val operandRight: Node
    ) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitBinary(this)

        enum class Operator(val symbol: TokenType.Symbol, vararg val instructions: Bytecode.Instruction) {
            EQUAL(
                TokenType.Symbol.DOUBLE_EQUAL,
                Bytecode.Instruction.EQU
            ),

            NOT_EQUAL(
                TokenType.Symbol.EXCLAMATION_EQUAL,
                Bytecode.Instruction.EQU, Bytecode.Instruction.NOT
            ),

            LESS(
                TokenType.Symbol.LESS,
                Bytecode.Instruction.GEQ, Bytecode.Instruction.NOT
            ),

            LESS_EQUAL(
                TokenType.Symbol.LESS_EQUAL,
                Bytecode.Instruction.GRT, Bytecode.Instruction.NOT
            ),

            GREATER(
                TokenType.Symbol.GREATER,
                Bytecode.Instruction.GRT
            ),

            GREATER_EQUAL(
                TokenType.Symbol.GREATER_EQUAL,
                Bytecode.Instruction.GEQ
            ),

            ADD(
                TokenType.Symbol.PLUS,
                Bytecode.Instruction.ADD
            ),

            SUBTRACT(
                TokenType.Symbol.DASH,
                Bytecode.Instruction.SUB
            ),

            MULTIPLY(
                TokenType.Symbol.STAR,
                Bytecode.Instruction.MUL
            ),

            DIVIDE(
                TokenType.Symbol.SLASH,
                Bytecode.Instruction.DIV
            ) {
                override val intVersion get() = INT_DIVIDE
            },

            INT_DIVIDE(
                TokenType.Symbol.SLASH,
                Bytecode.Instruction.IDIV
            ),

            MODULUS(
                TokenType.Symbol.PERCENT,
                Bytecode.Instruction.MOD
            ) {
                override val intVersion get() = INT_MODULUS
            },

            INT_MODULUS(
                TokenType.Symbol.PERCENT,
                Bytecode.Instruction.IMOD
            );

            open val intVersion get() = this

            companion object {
                operator fun get(symbol: TokenType) =
                    entries.first { it.symbol == symbol }
            }
        }
    }

    class Logical(
        context: Context,
        val operator: Operator,
        val operandLeft: Node,
        val operandRight: Node
    ) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitLogical(this)

        enum class Operator(val symbol: TokenType.Symbol, val instruction: Bytecode.Instruction) {
            OR(
                TokenType.Symbol.DOUBLE_PIPE,
                Bytecode.Instruction.OR
            ),

            AND(
                TokenType.Symbol.DOUBLE_AMPERSAND,
                Bytecode.Instruction.AND
            );

            companion object {
                operator fun get(symbol: TokenType) =
                    entries.first { it.symbol == symbol }
            }
        }
    }

    class Assign(context: Context, val name: Name, val assigned: Node) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitAssign(this)
    }

    class Invoke(
        context: Context,
        val name: Name,
        val args: Nodes
    ) : Node(context) {
        var isNative = false
        var id = -1
        var offset = 0

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitInvoke(this)
    }

    class GetIndex(context: Context, val name: Name, val indices: List<Node>) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitGetIndex(this)
    }

    class SetIndex(
        context: Context,
        val name: Name,
        val indices: List<Node>,
        val value: Node
    ) : Node(context) {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSetIndex(this)
    }
}