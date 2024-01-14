package kakkoiichris.stackvm.lang

import kakkoiichris.stackvm.asm.ASMTokenType

class ASMConverter(private val parser: Parser) : Node.Visitor<List<ASMTokenType>> {
    override fun visitIf(node: Node.If): List<ASMTokenType> {
        TODO("Not yet implemented")
    }

    override fun visitWhile(node: Node.While): List<ASMTokenType> {
        TODO("Not yet implemented")
    }

    override fun visitBreak(node: Node.Break): List<ASMTokenType> {
        TODO("Not yet implemented")
    }

    override fun visitContinue(node: Node.Continue): List<ASMTokenType> {
        TODO("Not yet implemented")
    }

    override fun visitExpression(node: Node.Expression): List<ASMTokenType> {
        TODO("Not yet implemented")
    }

    override fun visitValue(node: Node.Value): List<ASMTokenType> {
        TODO("Not yet implemented")
    }

    override fun visitName(node: Node.Name): List<ASMTokenType> {
        TODO("Not yet implemented")
    }

    override fun visitUnary(node: Node.Unary): List<ASMTokenType> {
        TODO("Not yet implemented")
    }

    override fun visitBinary(node: Node.Binary): List<ASMTokenType> {
        TODO("Not yet implemented")
    }

    override fun visitAssign(node: Node.Assign): List<ASMTokenType> {
        TODO("Not yet implemented")
    }
}