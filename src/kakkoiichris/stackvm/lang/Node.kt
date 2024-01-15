package kakkoiichris.stackvm.lang

interface Node {
    val location: Location

    fun <X> accept(visitor: Visitor<X>): X

    interface Visitor<X> {
        fun visit(node: Node) =
            node.accept(this)

        fun visitIf(node: If): X

        fun visitWhile(node: While): X

        fun visitBreak(node: Break): X

        fun visitContinue(node: Continue): X

        fun visitExpression(node: Expression): X

        fun visitValue(node: Value): X

        fun visitName(node: Name): X

        fun visitUnary(node: Unary): X

        fun visitBinary(node: Binary): X

        fun visitAssign(node: Assign): X
    }

    class If(override val location: Location, val condition: Node, val body: List<Node>) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitIf(this)
    }

    class While(override val location: Location, val condition: Node, val body: List<Node>) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitWhile(this)
    }

    class Break(override val location: Location) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitBreak(this)
    }

    class Continue(override val location: Location) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitContinue(this)
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
    ) :
        Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitBinary(this)
    }

    class Assign(override val location: Location, val name: Name, val node: Node) : Node {
        override fun <X> accept(visitor: Visitor<X>): X =
            visitor.visitAssign(this)
    }
}