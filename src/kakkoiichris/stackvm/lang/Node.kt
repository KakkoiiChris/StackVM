package stackvm.lang

interface Node {
    fun <X> accept(visitor: Visitor<X>): X
}

interface Visitor<X> {
    fun visit(node: Node) =
        node.accept(this)
}

class If() : Node {
    override fun <X> accept(visitor: Visitor<X>): X=
        visitor.visitIf(this)
}

class While() : Node {
    override fun <X> accept(visitor: Visitor<X>): X=
        visitor.visitWhile(this)
}

class Break() : Node {
    override fun <X> accept(visitor: Visitor<X>): X =
        visitor.visitBreak(this)
}

class Continue() : Node {
    override fun <X> accept(visitor: Visitor<X>): X =
        visitor.visitContinue(this)
}

class Expression() : Node {
    override fun <X> accept(visitor: Visitor<X>): X =
        visitor.visitExpression(this)
}

class Unary() : Node {
    override fun <X> accept(visitor: Visitor<X>): X =
        visitor.visitUnary(this)
}

class Binary() : Node {
    override fun <X> accept(visitor: Visitor<X>): X =
        visitor.visitBinary(this)
}

class Assign() : Node {
    override fun <X> accept(visitor: Visitor<X>): X =
        visitor.visitAssign(this)
}