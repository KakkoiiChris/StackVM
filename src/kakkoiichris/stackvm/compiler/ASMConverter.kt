package kakkoiichris.stackvm.compiler

import kakkoiichris.stackvm.asm.ASMToken
import kakkoiichris.stackvm.asm.ASMToken.Instruction.*
import kakkoiichris.stackvm.lang.DataType
import kakkoiichris.stackvm.lang.Node
import kakkoiichris.stackvm.lang.Parser

class ASMConverter(private val parser: Parser, private val optimize: Boolean) : Node.Visitor<List<IASMToken>> {
    private var pos = 0

    private val functions = mutableMapOf<Int, Int>()

    fun convert(): List<ASMToken> {
        try {
            parser.open()

            val tokens = mutableListOf<ASMToken>()

            for (statement in parser) {
                val iTokens = visit(statement)

                val subTokens = iTokens.filterIsInstance<IASMToken.Ok>()

                if (iTokens.size > subTokens.size) error("Unresolved intermediate token.")

                tokens.addAll(subTokens.map { it.token })
            }

            tokens.add(HALT)

            if (optimize) {
                var i = 0

                while (i < tokens.lastIndex) {
                    val a = tokens[i]
                    val b = tokens[i + 1]

                    if (a === b && a in listOf(NOT, NEG)) {
                        repeat(2) { tokens.removeAt(i) }
                    }
                    else {
                        i++
                    }
                }
            }

            return tokens
        }
        finally {
            parser.close()
        }
    }

    private fun resolveStartAndEnd(iTokens: List<IASMToken>, start: Float, end: Float) =
        iTokens
            .map { it.resolveStartAndEnd(start, end) ?: it }
            .toMutableList()

    private fun resolveLabelStartAndEnd(iTokens: List<IASMToken>, label: Node.Name, start: Float, end: Float) =
        iTokens
            .map { it.resolveLabelStartAndEnd(label, start, end) ?: it }
            .toMutableList()

    private fun resolveLast(iTokens: List<IASMToken>, last: Float) =
        iTokens
            .map { it.resolveLast(last) ?: it }
            .toMutableList()

    override fun visitDeclareSingle(node: Node.DeclareSingle): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.node)

        iTokens += STORE.iasm
        iTokens += ASMToken.Value(node.address.toFloat()).iasm

        pos += 2

        return iTokens
    }

    override fun visitDeclareArray(node: Node.DeclareArray): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.node)

        iTokens += ASTORE.iasm
        iTokens += ASMToken.Value(node.address.toFloat()).iasm

        pos += 2

        return iTokens
    }

    override fun visitIf(node: Node.If): List<IASMToken> {
        var iTokens = mutableListOf<IASMToken>()

        var last = -1F

        for ((i, branch) in node.branches.withIndex()) {
            val (_, condition, body) = branch

            val start = pos.toFloat()

            if (condition != null) {
                iTokens += visit(condition)

                iTokens += NOT.iasm
                iTokens += JIF.iasm
                iTokens += IASMToken.AwaitEnd()

                pos += 3
            }

            for (stmt in body) {
                iTokens += visit(stmt)
            }

            if (i != node.branches.lastIndex) {
                iTokens += JMP.iasm
                iTokens += IASMToken.AwaitLast()

                pos += 2
            }

            val end = pos.toFloat()
            last = end

            iTokens = resolveStartAndEnd(iTokens, start, end)
        }

        iTokens = resolveLast(iTokens, last)

        return iTokens
    }

    override fun visitWhile(node: Node.While): List<IASMToken> {
        var iTokens = mutableListOf<IASMToken>()

        val start = pos.toFloat()

        iTokens += visit(node.condition)

        iTokens += NOT.iasm
        iTokens += JIF.iasm
        iTokens += IASMToken.AwaitEnd()

        pos += 3

        for (stmt in node.body) {
            iTokens += visit(stmt)
        }

        iTokens += JMP.iasm
        iTokens += IASMToken.AwaitStart()

        pos += 2

        val end = pos.toFloat()

        iTokens = resolveStartAndEnd(iTokens, start, end)

        val label = node.label

        if (label != null) {
            iTokens = resolveLabelStartAndEnd(iTokens, label, start, end)
        }

        return iTokens
    }

    override fun visitDo(node: Node.Do): List<IASMToken> {
        var iTokens = mutableListOf<IASMToken>()

        val start = pos.toFloat()

        for (stmt in node.body) {
            iTokens += visit(stmt)
        }

        iTokens += visit(node.condition)

        iTokens += JIF.iasm
        iTokens += IASMToken.AwaitStart()

        pos += 2

        val end = pos.toFloat()

        iTokens = resolveStartAndEnd(iTokens, start, end)

        val label = node.label

        if (label != null) {
            iTokens = resolveLabelStartAndEnd(iTokens, label, start, end)
        }

        return iTokens
    }

    override fun visitFor(node: Node.For): List<IASMToken> {
        var iTokens = mutableListOf<IASMToken>()

        if (node.init != null) {
            iTokens += visit(node.init)
        }

        val start = pos.toFloat()

        if (node.condition != null) {
            iTokens += visit(node.condition)

            iTokens += NOT.iasm
            iTokens += JIF.iasm
            iTokens += IASMToken.AwaitEnd()

            pos += 3
        }

        for (stmt in node.body) {
            iTokens += visit(stmt)
        }

        if (node.increment != null) {
            iTokens += visit(node.increment)

            iTokens += POP.iasm

            pos++
        }

        iTokens += JMP.iasm
        iTokens += IASMToken.AwaitStart()

        pos += 2

        val end = pos.toFloat()

        iTokens = resolveStartAndEnd(iTokens, start, end)

        if (node.label != null) {
            iTokens = resolveLabelStartAndEnd(iTokens, node.label, start, end)
        }

        return iTokens
    }

    override fun visitBreak(node: Node.Break): List<IASMToken> {
        val label = node.label

        val iTokens = mutableListOf<IASMToken>()

        iTokens += JMP.iasm
        iTokens += if (label != null) IASMToken.AwaitLabelEnd(label) else IASMToken.AwaitEnd()

        pos += 2

        return iTokens
    }

    override fun visitContinue(node: Node.Continue): List<IASMToken> {
        val label = node.label

        val iTokens = mutableListOf<IASMToken>()

        iTokens += JMP.iasm
        iTokens += if (label != null) IASMToken.AwaitLabelStart(label) else IASMToken.AwaitStart()

        pos += 2

        return iTokens
    }

    override fun visitFunction(node: Node.Function): List<IASMToken> {
        var iTokens = mutableListOf<IASMToken>()

        iTokens += JMP.iasm
        iTokens += IASMToken.AwaitEnd()

        pos += 2

        val start = pos.toFloat()

        functions[node.id] = pos

        iTokens += FRAME.iasm
        iTokens += ASMToken.Value(node.offset.toFloat()).iasm

        pos += 2

        for (param in node.params) {
            iTokens += STORE.iasm
            iTokens += ASMToken.Value(param.address.toFloat()).iasm

            pos += 2
        }

        for (stmt in node.body) {
            iTokens += visit(stmt)
        }

        if (iTokens.none { it is IASMToken.Ok && it.token == RET }) {
            iTokens += PUSH.iasm
            iTokens += ASMToken.Value(0F).iasm
            iTokens += RET.iasm

            pos += 3
        }

        val end = pos.toFloat()

        iTokens = resolveStartAndEnd(iTokens, start, end)

        return iTokens
    }

    override fun visitReturn(node: Node.Return): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        val subNode = node.node

        if (subNode != null) {
            iTokens += visit(subNode)
        }

        iTokens += RET.iasm

        pos++

        return iTokens
    }

    override fun visitExpression(node: Node.Expression): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.node)

        iTokens += POP.iasm

        pos++

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
        error("Should not visit Name!")
    }

    override fun visitVariable(node: Node.Variable): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += when (node.dataType) {
            is DataType.Array -> node.mode.aLoad

            else              -> node.mode.load
        }.iasm
        iTokens += ASMToken.Value(node.address.toFloat()).iasm

        pos += 2

        return iTokens
    }

    override fun visitType(node: Node.Type): List<IASMToken> {
        error("Should not visit Type!")
    }

    override fun visitArray(node: Node.Array): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        for (element in node.elements.reversed()) {
            iTokens += visit(element)
        }

        iTokens += PUSH.iasm
        iTokens += ASMToken.Value(node.dataType.offset.toFloat() - 1).iasm

        pos += 2

        return iTokens
    }

    override fun visitUnary(node: Node.Unary): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.operand)

        iTokens += node.operator.instruction.iasm

        pos++

        return iTokens
    }

    override fun visitBinary(node: Node.Binary): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.operandLeft)
        iTokens += visit(node.operandRight)

        val additional = node.operator.instructions.map { it.iasm }
        iTokens += additional

        pos += additional.size

        return iTokens
    }

    override fun visitAssign(node: Node.Assign): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.node)

        iTokens += DUP.iasm
        iTokens += STORE.iasm
        iTokens += ASMToken.Value(node.address.toFloat()).iasm

        pos += 3

        return iTokens
    }

    override fun visitInvoke(node: Node.Invoke): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        for (arg in node.args.reversed()) {
            iTokens += visit(arg)
        }

        val address = functions[node.id] ?: error("Function does not exist!")

        iTokens += CALL.iasm
        iTokens += ASMToken.Value(address.toFloat()).iasm

        pos += 2

        return iTokens
    }

    override fun visitSystemCall(node: Node.SystemCall): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        for (arg in node.args.reversed()) {
            iTokens += visit(arg)
        }

        iTokens += SYS.iasm
        iTokens += ASMToken.Value(node.id.toFloat()).iasm

        pos += 2

        return iTokens
    }

    override fun visitGetIndex(node: Node.GetIndex): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        val origin = node.variable.address

        val indices = node
            .indices
            .reversed()
            .map { visit(it) }

        for (index in indices) {
            iTokens += index
        }

        iTokens += if (node.indices.size < node.arrayType.dimension) {
            node.variable.mode.iaLoad
        }
        else {
            node.variable.mode.iLoad
        }.iasm
        iTokens += ASMToken.Value(origin.toFloat()).iasm
        iTokens += ASMToken.Value(indices.size.toFloat()).iasm
        pos += 3

        return iTokens
    }

    override fun visitSetIndex(node: Node.SetIndex): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        val origin = node.variable.address

        val indices = node
            .indices
            .reversed()
            .map { visit(it) }

        iTokens += visit(node.value)

        iTokens += DUP.iasm
        pos++

        for (index in indices) {
            iTokens += index
        }

        iTokens += if (node.indices.size < node.arrayType.dimension) {
            IASTORE
        }
        else {
            ISTORE
        }.iasm
        iTokens += ASMToken.Value(origin.toFloat()).iasm
        iTokens += ASMToken.Value(indices.size.toFloat()).iasm
        pos += 3

        return iTokens
    }
}