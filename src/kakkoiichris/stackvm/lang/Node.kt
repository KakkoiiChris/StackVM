package kakkoiichris.stackvm.lang

typealias Nodes = List<Node>

interface Node {
    val location: Location

    fun <X> accept(visitor: Visitor<X>): X

    interface Visitor<X> {
        fun visit(node: Node) =
            node.accept(this)

        fun visitDeclare(node: Declare): X

        fun visitIf(node: If): X

        fun visitWhile(node: While): X

        fun visitBreak(node: Break): X

        fun visitContinue(node: Continue): X

        fun visitFunction(node: Function): X

        fun visitReturn(node: Return): X

        fun visitExpression(node: Expression): X

        fun visitValue(node: Value): X

        fun visitName(node: Name): X

        fun visitUnary(node: Unary): X

        fun visitBinary(node: Binary): X

        fun visitAssign(node: Assign): X

        fun visitInvoke(node: Invoke): X

        fun visitSystemCall(node: SystemCall): X
    }

    class Declare(override val location: Location, val constant: Boolean, val name: Name, val node: Node) : Node {
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
        val params: List<Name>,
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

    class Name(override val location: Location, val name: TokenType.Name) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitName(this)
    }

    class Unary(override val location: Location, val operator: TokenType.Symbol, val operand: Node) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitUnary(this)
    }

    class Binary(
        override val location: Location,
        val operator: TokenType.Symbol,
        val operandLeft: Node,
        val operandRight: Node
    ) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitBinary(this)
    }

    class Assign(override val location: Location, val name: Name, val node: Node) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitAssign(this)
    }

    class Invoke(override val location: Location, val name: Name, val args: Nodes) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitInvoke(this)
    }

    class SystemCall(override val location: Location, val name: Name, val args: Nodes) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitSystemCall(this)
    }
}