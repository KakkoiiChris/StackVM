package kakkoiichris.stackvm.lang

import kakkoiichris.stackvm.asm.ASMToken
import kakkoiichris.stackvm.lang.DataType.Primitive

typealias Nodes = List<Node>

interface Node {
    val location: Location

    val dataType: DataType get() = Primitive.VOID

    val subNodes: List<Node> get() = emptyList()

    val isOrHasReturns get() = false

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
        val type: Type,
        val node: Node
    ) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitDeclare(this)
    }

    class If(override val location: Location, val branches: List<Branch>) : Node {
        override val subNodes get() = branches.flatMap { it.body }

        override val isOrHasReturns get() = branches.any { branch -> branch.body.any { it.isOrHasReturns } }

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
        override val subNodes get() = body

        override val isOrHasReturns get() = body.any { it.isOrHasReturns }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitWhile(this)
    }

    class Do(override val location: Location, val label: Name?, val body: Nodes, val condition: Node) : Node {
        override val subNodes get() = body

        override val isOrHasReturns get() = body.any { it.isOrHasReturns }

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
        override val subNodes get() = body

        override val isOrHasReturns get() = body.any { it.isOrHasReturns }

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
        val type: Type,
        val body: Nodes,
        val isNative: Boolean
    ) : Node {
        override val dataType get() = type.dataType

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitFunction(this)
    }

    class Return(override val location: Location, val node: Node?) : Node {
        override val dataType get() = node?.dataType ?: Primitive.VOID

        override val isOrHasReturns get() = true

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitReturn(this)
    }

    class Expression(override val location: Location, val node: Node) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitExpression(this)
    }

    class Value(override val location: Location, val value: TokenType.Value) : Node {
        override val dataType get() = value.dataType

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitValue(this)
    }

    class Variable(
        override val location: Location,
        val name: TokenType.Name,
        val address: Int,
        val mode: Memory.Lookup.Mode,
        override val dataType: DataType
    ) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitVariable(this)
    }

    class Type(override val location: Location, val type: TokenType.Type) : Node {
        override val dataType get() = type.value

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitType(this)
    }

    class Unary(override val location: Location, val operator: Operator, val operand: Node) : Node {
        override val dataType: DataType
            get() {
                val type = operand.dataType

                return when (operator) {
                    Operator.NEGATE -> when (type) {
                        Primitive.FLOAT -> Primitive.FLOAT

                        Primitive.INT   -> Primitive.INT

                        else            -> error("Operand of type '$type' invalid for '$operator' operator @ ${operand.location}!")
                    }

                    Operator.INVERT -> when (type) {
                        Primitive.BOOL -> Primitive.BOOL

                        else           -> error("Operand of type '$type' invalid for '$operator' operator @ ${operand.location}!")
                    }
                }
            }

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
        override val dataType: DataType
            get() {
                val typeLeft = operandLeft.dataType
                val typeRight = operandRight.dataType

                return when (operator) {
                    Operator.OR            -> when (typeLeft) {
                        Primitive.BOOL -> when (typeRight) {
                            Primitive.BOOL -> Primitive.BOOL

                            else           -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else           -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.AND           -> when (typeLeft) {
                        Primitive.BOOL -> when (typeRight) {
                            Primitive.BOOL -> Primitive.BOOL

                            else           -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else           -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.EQUAL         -> Primitive.BOOL

                    Operator.NOT_EQUAL     -> Primitive.BOOL

                    Operator.LESS          -> Primitive.BOOL

                    Operator.LESS_EQUAL    -> Primitive.BOOL

                    Operator.GREATER       -> Primitive.BOOL

                    Operator.GREATER_EQUAL -> Primitive.BOOL

                    Operator.ADD           -> when (typeLeft) {
                        Primitive.INT   -> when (typeRight) {
                            Primitive.INT   -> Primitive.INT

                            Primitive.FLOAT -> Primitive.FLOAT

                            else            -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        Primitive.FLOAT -> when (typeRight) {
                            Primitive.INT   -> Primitive.FLOAT

                            Primitive.FLOAT -> Primitive.FLOAT

                            else            -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        Primitive.CHAR  -> when (typeRight) {
                            Primitive.INT -> Primitive.CHAR

                            else          -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else            -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.SUBTRACT      -> when (typeLeft) {
                        Primitive.INT   -> when (typeRight) {
                            Primitive.INT   -> Primitive.INT

                            Primitive.FLOAT -> Primitive.FLOAT

                            else            -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        Primitive.FLOAT -> when (typeRight) {
                            Primitive.INT   -> Primitive.FLOAT

                            Primitive.FLOAT -> Primitive.FLOAT

                            else            -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        Primitive.CHAR  -> when (typeRight) {
                            Primitive.INT -> Primitive.CHAR

                            else          -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else            -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.MULTIPLY      -> when (typeLeft) {
                        Primitive.INT   -> when (typeRight) {
                            Primitive.INT   -> Primitive.INT

                            Primitive.FLOAT -> Primitive.FLOAT

                            else            -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        Primitive.FLOAT -> when (typeRight) {
                            Primitive.INT   -> Primitive.FLOAT

                            Primitive.FLOAT -> Primitive.FLOAT

                            else            -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else            -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.DIVIDE        -> when (typeLeft) {
                        Primitive.INT   -> when (typeRight) {
                            Primitive.FLOAT -> Primitive.FLOAT

                            else            -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        Primitive.FLOAT -> when (typeRight) {
                            Primitive.INT   -> Primitive.FLOAT

                            Primitive.FLOAT -> Primitive.FLOAT

                            else            -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else            -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.INT_DIVIDE    -> when (typeLeft) {
                        Primitive.INT -> when (typeRight) {
                            Primitive.INT -> Primitive.INT

                            else          -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else          -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.MODULUS       -> when (typeLeft) {
                        Primitive.INT   -> when (typeRight) {
                            Primitive.FLOAT -> Primitive.FLOAT

                            else            -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        Primitive.FLOAT -> when (typeRight) {
                            Primitive.INT   -> Primitive.FLOAT

                            Primitive.FLOAT -> Primitive.FLOAT

                            else            -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else            -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.INT_MODULUS   -> when (typeLeft) {
                        Primitive.INT -> when (typeRight) {
                            Primitive.INT -> Primitive.INT

                            else          -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else          -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }
                }
            }

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
            ) {
                override val intVersion get() = INT_DIVIDE
            },

            INT_DIVIDE(
                TokenType.Symbol.SLASH,
                ASMToken.Instruction.IDIV
            ),

            MODULUS(
                TokenType.Symbol.PERCENT,
                ASMToken.Instruction.MOD
            ) {
                override val intVersion get() = INT_MODULUS
            },

            INT_MODULUS(
                TokenType.Symbol.PERCENT,
                ASMToken.Instruction.IMOD
            );

            open val intVersion get() = this

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

    class Invoke(
        override val location: Location,
        val name: Name,
        override val dataType: DataType,
        val id: Int,
        val args: Nodes
    ) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitInvoke(this)
    }

    class SystemCall(
        override val location: Location,
        val name: Name,
        override val dataType: DataType,
        val id: Int,
        val args: Nodes
    ) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSystemCall(this)
    }

    class Name(override val location: Location, val name: TokenType.Name) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitName(this)

        override fun equals(other: Any?): Boolean {
            if (other !is Name) return false

            return name.value == other.name.value
        }

        override fun hashCode(): Int {
            var result = location.hashCode()
            result = 31 * result + name.value.hashCode()
            return result
        }
    }
}