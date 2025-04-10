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

import kakkoiichris.svml.lang.Source
import kakkoiichris.svml.lang.compiler.Bytecode
import kakkoiichris.svml.lang.lexer.Context
import kakkoiichris.svml.lang.lexer.TokenType
import kakkoiichris.svml.lang.parser.DataType.Primitive.*

typealias Nodes = List<Node>

interface Node {
    val context: Context

    val subNodes: List<Node> get() = emptyList()

    val isOrHasReturns get() = false

    fun getDataType(source: Source): DataType? = VOID

    fun <X> accept(visitor: Visitor<X>): X

    interface Visitor<X> {
        fun visit(node: Node) =
            node.accept(this)

        fun visitProgram(node: Program): X

        fun visitDeclareSingle(node: DeclareSingle): X

        fun visitDeclareArray(node: DeclareArray): X

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

        fun visitVariable(node: Variable): X

        fun visitArray(node: Array): X

        fun visitUnary(node: Unary): X

        fun visitSize(node: Size): X

        fun visitIndexSize(node: IndexSize): X

        fun visitBinary(node: Binary): X

        fun visitLogical(node: Logical): X

        fun visitAssign(node: Assign): X

        fun visitInvoke(node: Invoke): X

        fun visitSystemCall(node: SystemCall): X

        fun visitGetIndex(node: GetIndex): X

        fun visitSetIndex(node: SetIndex): X
    }

    class Program(override val context: Context, val statements: Nodes, val mainReturn: Return) : Node {
        var offset = -1

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitProgram(this)
    }

    class DeclareSingle(
        override val context: Context,
        val variable: Variable,
        val id: Int,
        val node: Node?
    ) : Node {
        var address = -1

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitDeclareSingle(this)
    }

    class DeclareArray(
        override val context: Context,
        val variable: Variable,
        val id: Int,
        val node: Node?
    ) : Node {
        var address = -1

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitDeclareArray(this)
    }

    class If(override val context: Context, val branches: List<Branch>) : Node {
        override val subNodes get() = branches.flatMap { it.body }

        override val isOrHasReturns get() = branches.any { branch -> branch.body.any { it.isOrHasReturns } }

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

    class While(override val context: Context, val condition: Node, val label: Name?, val body: Nodes) : Node {
        override val subNodes get() = body

        override val isOrHasReturns get() = body.any { it.isOrHasReturns }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitWhile(this)
    }

    class Do(override val context: Context, val label: Name?, val body: Nodes, val condition: Node) : Node {
        override val subNodes get() = body

        override val isOrHasReturns get() = body.any { it.isOrHasReturns }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitDo(this)
    }

    class For(
        override val context: Context,
        val init: DeclareSingle?,
        val condition: Node?,
        val increment: Node?,
        val label: Name?,
        val body: Nodes
    ) : Node {
        override val subNodes get() = body

        override val isOrHasReturns get() = body.any { it.isOrHasReturns }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitFor(this)
    }

    class Break(override val context: Context, val label: Name?) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitBreak(this)
    }

    class Continue(override val context: Context, val label: Name?) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitContinue(this)
    }

    class Function(
        override val context: Context,
        val name: Name,
        val params: List<Variable>,
        val dataType: DataType,
        val isNative: Boolean,
        val body: Nodes
    ) : Node {
        var offset = -1

        val signature get() = Signature(name, params.map { it.dataType })

        val id = signature.hashCode()

        override fun getDataType(source: Source) = dataType

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitFunction(this)
    }

    class Return(override val context: Context, val node: Node?) : Node {
        override fun getDataType(source: Source) = node?.getDataType(source) ?: VOID

        override val isOrHasReturns get() = true

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitReturn(this)
    }

    class Expression(override val context: Context, val node: Node) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitExpression(this)
    }

    class Value(override val context: Context, val value: TokenType.Value) : Node {
        override fun getDataType(source: Source) = value.dataType

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitValue(this)
    }

    class String(override val context: Context, val value: kotlin.String) : Node {
        override fun getDataType(source: Source) = DataType.Alias(Name(context, TokenType.Name("string")))

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitString(this)
    }

    class Variable(
        override val context: Context,
        val name: TokenType.Name,
        val id: Int,
        val isGlobal: Boolean,
        val dataType: DataType
    ) : Node {
        var address = -1

        override fun getDataType(source: Source) = dataType

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitVariable(this)
    }

    class Array(override val context: Context, val elements: Nodes) : Node {
        override fun getDataType(source: Source): DataType {
            val firstType =
                elements.firstOrNull()?.getDataType(source)
                    ?: error("Type of empty array cannot be inferred @ $context!")

            for (element in elements.drop(1)) {
                val elementType = element.getDataType(source)

                if (elementType != firstType) {
                    error("Array of type '$firstType' cannot store value of type '$elementType' @ ${element.context}!")
                }
            }

            return DataType.Array(firstType, elements.size)
        }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitArray(this)
    }

    class Unary(override val context: Context, val operator: Operator, val operand: Node) : Node {
        override fun getDataType(source: Source): DataType {
            val type = operand.getDataType(source)

            return when (operator) {
                Operator.NEGATE -> when (type) {
                    FLOAT -> FLOAT

                    INT   -> INT

                    else  -> error("Operand of type '$type' invalid for '$operator' operator @ ${operand.context}!")
                }

                Operator.INVERT -> when (type) {
                    BOOL -> BOOL

                    else -> error("Operand of type '$type' invalid for '$operator' operator @ ${operand.context}!")
                }
            }
        }

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

    class Size(override val context: Context, val variable: Variable) : Node {
        override fun getDataType(source: Source) = DataType.Primitive.INT

        fun getArrayType(source: Source): DataType.Array {
            if (variable.dataType is DataType.Alias) {
                return DataType.getAlias(variable.dataType.name, source) as DataType.Array
            }

            return variable.dataType as DataType.Array
        }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSize(this)
    }

    class IndexSize(override val context: Context, val variable: Variable, val indices: List<Node>) : Node {
        override fun getDataType(source: Source) = DataType.Primitive.INT

        fun getArrayType(source: Source): DataType.Array {
            if (variable.dataType is DataType.Alias) {
                return DataType.getAlias(variable.dataType.name, source) as DataType.Array
            }

            return variable.dataType as DataType.Array
        }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitIndexSize(this)
    }

    class Binary(
        override val context: Context,
        val operator: Operator,
        val operandLeft: Node,
        val operandRight: Node
    ) : Node {
        override fun getDataType(source: Source): DataType {
            val typeLeft = operandLeft.getDataType(source)
            val typeRight = operandRight.getDataType(source)

            return when (operator) {
                Operator.EQUAL,
                Operator.NOT_EQUAL     -> when (typeLeft) {
                    BOOL  -> when (typeRight) {
                        BOOL -> BOOL

                        else -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    FLOAT -> when (typeRight) {
                        FLOAT, INT -> BOOL

                        else       -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    INT   -> when (typeRight) {
                        FLOAT, INT -> BOOL

                        else       -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    CHAR  -> when (typeRight) {
                        CHAR -> BOOL

                        else -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    else  -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.context}!")
                }

                Operator.LESS,
                Operator.LESS_EQUAL,
                Operator.GREATER,
                Operator.GREATER_EQUAL -> when (typeLeft) {
                    FLOAT -> when (typeRight) {
                        FLOAT, INT -> BOOL

                        else       -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    INT   -> when (typeRight) {
                        FLOAT, INT -> BOOL

                        else       -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    CHAR  -> when (typeRight) {
                        CHAR -> BOOL

                        else -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    else  -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.context}!")
                }

                Operator.ADD,
                Operator.SUBTRACT      -> when (typeLeft) {
                    INT   -> when (typeRight) {
                        INT   -> INT

                        FLOAT -> FLOAT

                        else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    FLOAT -> when (typeRight) {
                        INT   -> FLOAT

                        FLOAT -> FLOAT

                        else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    CHAR  -> when (typeRight) {
                        INT  -> CHAR

                        else -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    else  -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.context}!")
                }

                Operator.MULTIPLY      -> when (typeLeft) {
                    INT   -> when (typeRight) {
                        INT   -> INT

                        FLOAT -> FLOAT

                        else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    FLOAT -> when (typeRight) {
                        INT   -> FLOAT

                        FLOAT -> FLOAT

                        else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    else  -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.context}!")
                }

                Operator.DIVIDE,
                Operator.MODULUS       -> when (typeLeft) {
                    INT   -> when (typeRight) {
                        FLOAT -> FLOAT

                        else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    FLOAT -> when (typeRight) {
                        INT   -> FLOAT

                        FLOAT -> FLOAT

                        else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    else  -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.context}!")
                }

                Operator.INT_DIVIDE,
                Operator.INT_MODULUS   -> when (typeLeft) {
                    INT  -> when (typeRight) {
                        INT  -> INT

                        else -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.context}!")
                    }

                    else -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.context}!")
                }
            }
        }

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
        override val context: Context,
        val operator: Operator,
        val operandLeft: Node,
        val operandRight: Node
    ) : Node {
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

    class Assign(override val context: Context, val variable: Variable, val node: Node) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitAssign(this)
    }

    class Invoke(
        override val context: Context,
        val name: Name,
        val dataType: DataType,
        val id: Int,
        val args: Nodes
    ) : Node {
        var offset = 0

        override fun getDataType(source: Source) = dataType

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitInvoke(this)
    }

    class SystemCall(
        override val context: Context,
        val name: Name,
        val dataType: DataType,
        val id: Int,
        val args: Nodes
    ) : Node {
        override fun getDataType(source: Source) = dataType

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSystemCall(this)
    }

    class GetIndex(override val context: Context, val variable: Variable, val indices: List<Node>) : Node {
        override fun getDataType(source: Source): DataType? {
            var type = variable.dataType

            if (type is DataType.Alias) type = DataType.getAlias(type.name, source)

            var i = 0

            while (i < indices.size) {
                if (type !is DataType.Array) break

                type = type.subType

                i++
            }

            if (i < indices.size) return null

            return type
        }

        fun getArrayType(source: Source): DataType.Array {
            if (variable.dataType is DataType.Alias) {
                return DataType.getAlias(variable.dataType.name, source) as DataType.Array
            }

            return variable.dataType as DataType.Array
        }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitGetIndex(this)
    }

    class SetIndex(override val context: Context, val variable: Variable, val indices: List<Node>, val value: Node) :
        Node {
        fun getArrayType(source: Source) = DataType.asArray(variable.dataType, source)

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSetIndex(this)
    }

    data class Name(val context: Context, val name: TokenType.Name) {
        override fun equals(other: Any?) = when (other) {
            is Name   -> other.name.value == name.value

            is String -> other == name.value

            else      -> false
        }

        override fun hashCode(): Int {
            var result = context.hashCode()
            result = 31 * result + name.value.hashCode()
            return result
        }
    }
}