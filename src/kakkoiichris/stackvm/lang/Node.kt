package kakkoiichris.stackvm.lang

import kakkoiichris.stackvm.asm.ASMToken

typealias Nodes = List<Node>

interface Node {
    val location: Location

    val dataType: DataType get() = DataType.Primitive.VOID

    fun <X> accept(visitor: Visitor<X>): X

    interface Visitor<X> {
        fun visit(node: Node) =
            node.accept(this)

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

        fun visitVariable(node: Variable): X

        fun visitType(node: Type): X

        fun visitUnary(node: Unary): X

        fun visitBinary(node: Binary): X

        fun visitAssign(node: Assign): X

        fun visitInvoke(node: Invoke): X

        fun visitSystemCall(node: SystemCall): X

        fun visitName(node: Name): X
    }

    class Declare(
        override val location: Location,
        val constant: Boolean,
        val name: Variable,
        val address: Int,
        val node: Node
    ) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitDeclare(this)
    }

    class If(override val location: Location, val branches: List<Branch>) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitIf(this)

        fun toExpr(): If {
            val newBranches = mutableListOf<Branch>()

            for ((location, condition, body) in branches) {
                val newBody = body.toMutableList()

                val last = newBody.removeLast()

                if (last !is Expression) error("Last statement of if expression must be an expression!")

                newBody += last.node

                newBranches += Branch(location, condition, newBody)
            }

            return If(location, newBranches)
        }

        data class Branch(val location: Location, val condition: Node?, val body: Nodes)
    }

    class While(override val location: Location, val condition: Node, val label: Name?, val body: Nodes) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitWhile(this)
    }

    class Do(override val location: Location, val label: Name?, val body: Nodes, val condition: Node) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitDo(this)
    }

    class For(
        override val location: Location,
        val init: Declare?,
        val condition: Node?,
        val increment: Node?,
        val label: Name?,
        val body: Nodes
    ) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitFor(this)
    }

    class Break(override val location: Location, val label: Name?) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitBreak(this)
    }

    class Continue(override val location: Location, val label: Name?) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitContinue(this)
    }

    class Function(
        override val location: Location,
        val name: Name,
        val id: Int,
        val offset: Int,
        val params: List<Variable>,
        val body: Nodes
    ) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitFunction(this)
    }

    class Return(override val location: Location, val node: Node?) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitReturn(this)
    }

    class Expression(override val location: Location, val node: Node) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitExpression(this)
    }

    class Value(override val location: Location, val value: TokenType.Value) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitValue(this)
    }

    class Variable(
        override val location: Location,
        val name: TokenType.Name,
        val address: Int,
        val mode: Memory.Lookup.Mode
    ) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitVariable(this)
    }

    class Type(override val location: Location, val type: TokenType.Type) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitType(this)
    }

    class Unary(override val location: Location, val operator: Operator, val operand: Node) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitUnary(this)

        enum class Operator(val symbol: TokenType.Symbol, val instruction: ASMToken.Instruction) {
            NEGATE(
                TokenType.Symbol.DASH,
                ASMToken.Instruction.NEG
            ),

            INVERT(
                TokenType.Symbol.EXCLAMATION,
                ASMToken.Instruction.NOT
            );

            companion object {
                operator fun get(symbol: TokenType) =
                    entries.first { it.symbol == symbol }
            }
        }
    }

    class Binary(
        override val location: Location,
        val operator: Operator,
        val operandLeft: Node,
        val operandRight: Node
    ) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitBinary(this)

        enum class Operator(val symbol: TokenType.Symbol, vararg val instructions: ASMToken.Instruction) {
            OR(
                TokenType.Symbol.DOUBLE_PIPE,
                ASMToken.Instruction.OR
            ),

            AND(
                TokenType.Symbol.DOUBLE_AMPERSAND,
                ASMToken.Instruction.AND
            ),

            EQUAL(
                TokenType.Symbol.DOUBLE_EQUAL,
                ASMToken.Instruction.EQU
            ),

            NOT_EQUAL(
                TokenType.Symbol.EXCLAMATION_EQUAL,
                ASMToken.Instruction.EQU, ASMToken.Instruction.NOT
            ),

            LESS(
                TokenType.Symbol.LESS,
                ASMToken.Instruction.GEQ, ASMToken.Instruction.NOT
            ),

            LESS_EQUAL(
                TokenType.Symbol.LESS_EQUAL,
                ASMToken.Instruction.GRT, ASMToken.Instruction.NOT
            ),

            GREATER(
                TokenType.Symbol.GREATER,
                ASMToken.Instruction.GRT
            ),

            GREATER_EQUAL(
                TokenType.Symbol.GREATER_EQUAL,
                ASMToken.Instruction.GEQ
            ),

            ADD(
                TokenType.Symbol.PLUS,
                ASMToken.Instruction.ADD
            ),

            SUBTRACT(
                TokenType.Symbol.DASH,
                ASMToken.Instruction.SUB
            ),

            MULTIPLY(
                TokenType.Symbol.STAR,
                ASMToken.Instruction.MUL
            ),

            DIVIDE(
                TokenType.Symbol.SLASH,
                ASMToken.Instruction.DIV
            ),

            MODULUS(
                TokenType.Symbol.PERCENT,
                ASMToken.Instruction.MOD
            );

            companion object {
                operator fun get(symbol: TokenType) =
                    entries.first { it.symbol == symbol }
            }
        }
    }

    class Assign(override val location: Location, val name: Variable, val address: Int, val node: Node) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitAssign(this)
    }

    class Invoke(override val location: Location, val name: Name, val id: Int, val args: Nodes) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitInvoke(this)
    }

    class SystemCall(override val location: Location, val name: Name, val args: Nodes) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSystemCall(this)
    }

    class Name(override val location: Location, val name: TokenType.Name) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitName(this)
    }
}