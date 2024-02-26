package kakkoiichris.stackvm.lang.parser

import kakkoiichris.stackvm.lang.compiler.Bytecode
import kakkoiichris.stackvm.lang.lexer.Location
import kakkoiichris.stackvm.lang.lexer.TokenType
import kakkoiichris.stackvm.lang.parser.DataType.Primitive.*

typealias Nodes = List<Node>

interface Node {
    val location: Location

    val dataType: DataType? get() = VOID

    val subNodes: List<Node> get() = emptyList()

    val isOrHasReturns get() = false

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

        fun visitType(node: Type): X

        fun visitArray(node: Array): X

        fun visitUnary(node: Unary): X

        fun visitSize(node: Size): X

        fun visitIndexSize(node: IndexSize): X

        fun visitBinary(node: Binary): X

        fun visitAssign(node: Assign): X

        fun visitInvoke(node: Invoke): X

        fun visitSystemCall(node: SystemCall): X

        fun visitGetIndex(node: GetIndex): X

        fun visitSetIndex(node: SetIndex): X

        fun visitName(node: Name): X
    }

    class Program(override val location: Location, val statements: Nodes) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitProgram(this)
    }

    class DeclareSingle(
        override val location: Location,
        val variable: Variable,
        val id: Int,
        val node: Node?
    ) : Node {
        var address = -1

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitDeclareSingle(this)
    }

    class DeclareArray(
        override val location: Location,
        val variable: Variable,
        val id: Int,
        val node: Node?
    ) : Node {
        var address = -1

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitDeclareArray(this)
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

                if (last !is Expression) error("Last statement of if expression must be an expression @ ${last.location}!")

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
        val params: List<Variable>,
        override val dataType: DataType,
        val isNative: Boolean,
        val body: Nodes
    ) : Node {
        var offset = -1

        val signature get() = Signature(name, params.map { it.dataType })

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitFunction(this)
    }

    class Return(override val location: Location, val node: Node?) : Node {
        override val dataType get() = node?.dataType ?: VOID

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

    class String(override val location: Location, val value: TokenType.String) : Node {
        override val dataType get() = DataType.Alias(Name(location, TokenType.Name("string")))

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitString(this)
    }

    class Variable(
        override val location: Location,
        val name: TokenType.Name,
        val id: Int,
        val isGlobal: Boolean,
        override val dataType: DataType
    ) : Node {
        var address = -1

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitVariable(this)
    }

    class Type(override val location: Location, val type: TokenType.Type) : Node {
        override val dataType get() = type.value

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitType(this)
    }

    class Array(override val location: Location, val elements: Nodes) : Node {
        override val dataType: DataType
            get() {
                val firstType =
                    elements.firstOrNull()?.dataType ?: error("Type of empty array cannot be inferred @ $location!")

                for (element in elements.drop(1)) {
                    if (element.dataType != firstType) {
                        error("Array of type '$firstType' cannot store value of type '${element.dataType}' @ ${element.location}!")
                    }
                }

                return DataType.Array(firstType, elements.size)
            }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitArray(this)
    }

    class Unary(override val location: Location, val operator: Operator, val operand: Node) : Node {
        override val dataType: DataType
            get() {
                val type = operand.dataType

                return when (operator) {
                    Operator.NEGATE -> when (type) {
                        FLOAT -> FLOAT

                        INT   -> INT

                        else  -> error("Operand of type '$type' invalid for '$operator' operator @ ${operand.location}!")
                    }

                    Operator.INVERT -> when (type) {
                        BOOL -> BOOL

                        else -> error("Operand of type '$type' invalid for '$operator' operator @ ${operand.location}!")
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

    class Size(override val location: Location, val variable: Variable) : Node {
        override val dataType get() = DataType.Primitive.INT

        val arrayType: DataType.Array
            get() {
                if (variable.dataType is DataType.Alias) {
                    return DataType.getAlias(variable.dataType.name) as DataType.Array
                }

                return variable.dataType as DataType.Array
            }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSize(this)
    }

    class IndexSize(override val location: Location, val variable: Variable, val indices: List<Node>) : Node {
        override val dataType get() = DataType.Primitive.INT

        val arrayType: DataType.Array
            get() {
                if (variable.dataType is DataType.Alias) {
                    return DataType.getAlias(variable.dataType.name) as DataType.Array
                }

                return variable.dataType as DataType.Array
            }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitIndexSize(this)
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
                    Operator.OR,
                    Operator.AND           -> when (typeLeft) {
                        BOOL -> when (typeRight) {
                            BOOL -> BOOL

                            else -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.EQUAL,
                    Operator.NOT_EQUAL     -> when (typeLeft) {
                        BOOL  -> when (typeRight) {
                            BOOL -> BOOL

                            else -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        FLOAT -> when (typeRight) {
                            FLOAT, INT -> BOOL

                            else       -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        INT   -> when (typeRight) {
                            FLOAT, INT -> BOOL

                            else       -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        CHAR  -> when (typeRight) {
                            CHAR -> BOOL

                            else -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else  -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.LESS,
                    Operator.LESS_EQUAL,
                    Operator.GREATER,
                    Operator.GREATER_EQUAL -> when (typeLeft) {
                        FLOAT -> when (typeRight) {
                            FLOAT, INT -> BOOL

                            else       -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        INT   -> when (typeRight) {
                            FLOAT, INT -> BOOL

                            else       -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        CHAR  -> when (typeRight) {
                            CHAR -> BOOL

                            else -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else  -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.ADD,
                    Operator.SUBTRACT      -> when (typeLeft) {
                        INT   -> when (typeRight) {
                            INT   -> INT

                            FLOAT -> FLOAT

                            else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        FLOAT -> when (typeRight) {
                            INT   -> FLOAT

                            FLOAT -> FLOAT

                            else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        CHAR  -> when (typeRight) {
                            INT  -> CHAR

                            else -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else  -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.MULTIPLY      -> when (typeLeft) {
                        INT   -> when (typeRight) {
                            INT   -> INT

                            FLOAT -> FLOAT

                            else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        FLOAT -> when (typeRight) {
                            INT   -> FLOAT

                            FLOAT -> FLOAT

                            else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else  -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.DIVIDE,
                    Operator.MODULUS       -> when (typeLeft) {
                        INT   -> when (typeRight) {
                            FLOAT -> FLOAT

                            else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        FLOAT -> when (typeRight) {
                            INT   -> FLOAT

                            FLOAT -> FLOAT

                            else  -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else  -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }

                    Operator.INT_DIVIDE,
                    Operator.INT_MODULUS   -> when (typeLeft) {
                        INT  -> when (typeRight) {
                            INT  -> INT

                            else -> error("Right operand of type '$typeRight' invalid for '$operator' operator @ ${operandRight.location}!")
                        }

                        else -> error("Left operand of type '$typeLeft' invalid for '$operator' operator @ ${operandLeft.location}!")
                    }
                }
            }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitBinary(this)

        enum class Operator(val symbol: TokenType.Symbol, vararg val instructions: Bytecode.Instruction) {
            OR(
                TokenType.Symbol.DOUBLE_PIPE,
                Bytecode.Instruction.OR
            ),

            AND(
                TokenType.Symbol.DOUBLE_AMPERSAND,
                Bytecode.Instruction.AND
            ),

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

    class Assign(override val location: Location, val variable: Variable, val node: Node) : Node {
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

    class GetIndex(override val location: Location, val variable: Variable, val indices: List<Node>) : Node {
        override val dataType: DataType?
            get() {
                var type = variable.dataType

                if (type is DataType.Alias) type = DataType.getAlias(type.name)

                var i = 0

                while (i < indices.size) {
                    if (type !is DataType.Array) break

                    type = type.subType

                    i++
                }

                if (i < indices.size) return null

                return type
            }

        val arrayType: DataType.Array
            get() {
                if (variable.dataType is DataType.Alias) {
                    return DataType.getAlias(variable.dataType.name) as DataType.Array
                }

                return variable.dataType as DataType.Array
            }

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitGetIndex(this)
    }

    class SetIndex(override val location: Location, val variable: Variable, val indices: List<Node>, val value: Node) :
        Node {
        val arrayType get() = variable.dataType as DataType.Array

        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSetIndex(this)
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