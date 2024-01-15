package kakkoiichris.stackvm.lang

import kakkoiichris.stackvm.asm.ASMToken
import kakkoiichris.stackvm.asm.ASMToken.Keyword.*
import kakkoiichris.stackvm.lang.TokenType.Symbol.*
import java.util.*

class ASMConverter(private val parser: Parser) : Node.Visitor<List<IASMToken>> {
    private var pos = 0

    private val start = Stack<Int>()
    private val end = Stack<Int>()

    private val names = mutableListOf<String>()

    fun convert(): List<ASMToken> {
        val tokens = mutableListOf<ASMToken>()

        for (statement in parser) {
            val iTokens = visit(statement)

            val subTokens = iTokens.filterIsInstance(IASMToken.Ok::class.java)

            if (iTokens.size > subTokens.size) error("Unresolved intermediate token.")

            tokens.addAll(subTokens.map { it.token })
        }

        tokens.add(HALT)

        return tokens
    }

    private fun resolve(iTokens: List<IASMToken>): List<IASMToken> {
        val start = start.pop()
        val end = end.pop()

        return iTokens.map { it.resolve(start.toFloat(), end.toFloat()) }
    }

    override fun visitIf(node: Node.If): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        start.push(pos)

        iTokens += visit(node.condition)

        iTokens += NOT.iasm
        iTokens += JIF.iasm
        iTokens += IASMToken.AwaitEnd

        pos += 3

        for (stmt in node.body) {
            iTokens += visit(stmt)
        }

        end.push(pos)

        return resolve(iTokens)
    }

    override fun visitWhile(node: Node.While): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        start.push(pos)

        iTokens += visit(node.condition)

        iTokens += NOT.iasm
        iTokens += JIF.iasm
        iTokens += IASMToken.AwaitEnd

        pos += 3

        for (stmt in node.body) {
            iTokens += visit(stmt)
        }

        iTokens += JMP.iasm
        iTokens += IASMToken.AwaitStart

        pos += 2

        end.push(pos)

        return resolve(iTokens)
    }

    override fun visitBreak(node: Node.Break): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += JMP.iasm
        iTokens += IASMToken.AwaitEnd

        pos += 2

        return iTokens
    }

    override fun visitContinue(node: Node.Continue): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += JMP.iasm
        iTokens += IASMToken.AwaitStart

        pos += 2

        return iTokens
    }

    override fun visitExpression(node: Node.Expression): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += POP.iasm

        pos++

        iTokens += visit(node.node)

        return iTokens
    }

    override fun visitValue(node: Node.Value): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += PUSH.iasm
        iTokens += ASMToken.Value(node.value.value).iasm

        pos += 2

        return iTokens
    }

    override fun visitName(node: Node.Name): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        val name = node.name.value

        val address = names
            .indexOf(node.name.value)
            .takeIf { it >= 0 }
            ?: error("Undeclared variable name '$name' @ ${node.location}.")

        iTokens += LOAD.iasm
        iTokens += ASMToken.Value(address.toFloat()).iasm

        pos += 2

        return iTokens
    }

    override fun visitUnary(node: Node.Unary): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.operand)

        iTokens += when (node.operator) {
            DASH        -> NEG.iasm
            EXCLAMATION -> NOT.iasm
            else        -> error("Not a binary operator '${node.operator}'.")
        }

        pos++

        return iTokens
    }

    override fun visitBinary(node: Node.Binary): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.operandLeft)

        iTokens += visit(node.operandRight)

        val additional = when (node.operator) {
            PLUS              -> listOf(ADD.iasm)
            DASH              -> listOf(SUB.iasm)
            STAR              -> listOf(MUL.iasm)
            SLASH             -> listOf(DIV.iasm)
            PERCENT           -> listOf(MOD.iasm)
            LESS              -> listOf(GEQ.iasm, NOT.iasm)
            LESS_EQUAL        -> listOf(GRT.iasm, NOT.iasm)
            GREATER           -> listOf(GRT.iasm)
            GREATER_EQUAL     -> listOf(GEQ.iasm)
            DOUBLE_AMPERSAND  -> listOf(AND.iasm)
            DOUBLE_PIPE       -> listOf(OR.iasm)
            DOUBLE_EQUAL      -> listOf(EQU.iasm)
            EXCLAMATION_EQUAL -> listOf(EQU.iasm, NOT.iasm)
            else              -> error("Not a binary operator '${node.operator}'.")
        }

        pos += additional.size

        iTokens += additional

        return iTokens
    }

    override fun visitAssign(node: Node.Assign): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.node)

        val name = node.name.name.value

        val address = names
            .indexOf(name)
            .takeIf { it >= 0 }
            ?: (names.apply { add(name) }.size - 1)

        iTokens += STORE.iasm
        iTokens += ASMToken.Value(address.toFloat()).iasm
        iTokens += LOAD.iasm
        iTokens += ASMToken.Value(address.toFloat()).iasm

        pos += 4

        return iTokens
    }
}