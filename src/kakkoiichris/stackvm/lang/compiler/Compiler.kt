package kakkoiichris.stackvm.lang.compiler

import kakkoiichris.stackvm.lang.compiler.Bytecode.Instruction.*
import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.lang.parser.Node

class Compiler(private val program: Node.Program, private val optimize: Boolean) :
    Node.Visitor<List<IntermediateToken>> {
    private var pos = 0

    private val functions = mutableMapOf<Int, Int>()

    fun compile() =
        convert()
            .map { it.value }
            .toFloatArray()

    fun convert(): List<Bytecode> {
        val iTokens = visit(program).toMutableList()

        val subTokens = iTokens.filterIsInstance<IntermediateToken.Ok>()

        if (iTokens.size > subTokens.size) error("Unresolved intermediate token!")

        val tokens = subTokens
            .map { it.token }
            .toMutableList()
            .apply { add(HALT) }

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

    private fun resolveStartAndEnd(iTokens: List<IntermediateToken>, start: Float, end: Float) =
        iTokens
            .map { it.resolveStartAndEnd(start, end) ?: it }
            .toMutableList()

    private fun resolveLabelStartAndEnd(iTokens: List<IntermediateToken>, label: Node.Name, start: Float, end: Float) =
        iTokens
            .map { it.resolveLabelStartAndEnd(label, start, end) ?: it }
            .toMutableList()

    private fun resolveLast(iTokens: List<IntermediateToken>, last: Float) =
        iTokens
            .map { it.resolveLast(last) ?: it }
            .toMutableList()

    private val Float.intermediate get() = IntermediateToken.Ok(Bytecode.Value(this))

    override fun visitProgram(node: Node.Program): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        for (statement in node.statements) {
            iTokens += visit(statement)
        }

        return iTokens
    }

    override fun visitDeclareSingle(node: Node.DeclareSingle): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        if (node.node != null) {
            iTokens += visit(node.node)
        }

        iTokens += STORE.intermediate
        iTokens += node.address.toFloat().intermediate

        pos += 2

        return iTokens
    }

    override fun visitDeclareArray(node: Node.DeclareArray): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        if (node.node != null) {
            iTokens += visit(node.node)
        }
        else {
            val sizes = (node.variable.dataType as DataType.Array).sizes

            val da = getDefaultArray(sizes)

            iTokens += da
        }

        iTokens += ASTORE.intermediate
        iTokens += node.address.toFloat().intermediate

        pos += 2

        return iTokens
    }

    private fun getDefaultArray(sizes: IntArray) =
        getSubArray(sizes[0], sizes.drop(1).toIntArray())

    private fun getSubArray(size: Int, rest: IntArray): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        if (rest.isEmpty()) {
            repeat(size) {
                iTokens += PUSH.intermediate
                iTokens += 0F.intermediate

                pos += 2
            }

            iTokens += PUSH.intermediate
            iTokens += size.toFloat().intermediate

            pos += 2
        }
        else {
            var totalSize = 0

            repeat(size) {
                val subArray = getSubArray(rest[0], rest.drop(1).toIntArray())

                totalSize += subArray.size / 2

                iTokens += subArray
            }

            iTokens += PUSH.intermediate
            iTokens += totalSize.toFloat().intermediate

            pos += 2
        }

        return iTokens
    }

    override fun visitIf(node: Node.If): List<IntermediateToken> {
        var iTokens = mutableListOf<IntermediateToken>()

        var last = -1F

        for ((i, branch) in node.branches.withIndex()) {
            val (_, condition, body) = branch

            val start = pos.toFloat()

            if (condition != null) {
                iTokens += visit(condition)

                iTokens += NOT.intermediate
                iTokens += JIF.intermediate
                iTokens += IntermediateToken.AwaitEnd()

                pos += 3
            }

            for (stmt in body) {
                iTokens += visit(stmt)
            }

            if (i != node.branches.lastIndex) {
                iTokens += JMP.intermediate
                iTokens += IntermediateToken.AwaitLast()

                pos += 2
            }

            val end = pos.toFloat()
            last = end

            iTokens = resolveStartAndEnd(iTokens, start, end)
        }

        iTokens = resolveLast(iTokens, last)

        return iTokens
    }

    override fun visitWhile(node: Node.While): List<IntermediateToken> {
        var iTokens = mutableListOf<IntermediateToken>()

        val start = pos.toFloat()

        iTokens += visit(node.condition)

        iTokens += NOT.intermediate
        iTokens += JIF.intermediate
        iTokens += IntermediateToken.AwaitEnd()

        pos += 3

        for (stmt in node.body) {
            iTokens += visit(stmt)
        }

        iTokens += JMP.intermediate
        iTokens += IntermediateToken.AwaitStart()

        pos += 2

        val end = pos.toFloat()

        iTokens = resolveStartAndEnd(iTokens, start, end)

        val label = node.label

        if (label != null) {
            iTokens = resolveLabelStartAndEnd(iTokens, label, start, end)
        }

        return iTokens
    }

    override fun visitDo(node: Node.Do): List<IntermediateToken> {
        var iTokens = mutableListOf<IntermediateToken>()

        val start = pos.toFloat()

        for (stmt in node.body) {
            iTokens += visit(stmt)
        }

        iTokens += visit(node.condition)

        iTokens += JIF.intermediate
        iTokens += IntermediateToken.AwaitStart()

        pos += 2

        val end = pos.toFloat()

        iTokens = resolveStartAndEnd(iTokens, start, end)

        val label = node.label

        if (label != null) {
            iTokens = resolveLabelStartAndEnd(iTokens, label, start, end)
        }

        return iTokens
    }

    override fun visitFor(node: Node.For): List<IntermediateToken> {
        var iTokens = mutableListOf<IntermediateToken>()

        if (node.init != null) {
            iTokens += visit(node.init)
        }

        val start = pos.toFloat()

        if (node.condition != null) {
            iTokens += visit(node.condition)

            iTokens += NOT.intermediate
            iTokens += JIF.intermediate
            iTokens += IntermediateToken.AwaitEnd()

            pos += 3
        }

        for (stmt in node.body) {
            iTokens += visit(stmt)
        }

        if (node.increment != null) {
            iTokens += visit(node.increment)

            iTokens += POP.intermediate

            pos++
        }

        iTokens += JMP.intermediate
        iTokens += IntermediateToken.AwaitStart()

        pos += 2

        val end = pos.toFloat()

        iTokens = resolveStartAndEnd(iTokens, start, end)

        if (node.label != null) {
            iTokens = resolveLabelStartAndEnd(iTokens, node.label, start, end)
        }

        return iTokens
    }

    override fun visitBreak(node: Node.Break): List<IntermediateToken> {
        val label = node.label

        val iTokens = mutableListOf<IntermediateToken>()

        iTokens += JMP.intermediate
        iTokens += if (label != null) IntermediateToken.AwaitLabelEnd(label) else IntermediateToken.AwaitEnd()

        pos += 2

        return iTokens
    }

    override fun visitContinue(node: Node.Continue): List<IntermediateToken> {
        val label = node.label

        val iTokens = mutableListOf<IntermediateToken>()

        iTokens += JMP.intermediate
        iTokens += if (label != null) IntermediateToken.AwaitLabelStart(label) else IntermediateToken.AwaitStart()

        pos += 2

        return iTokens
    }

    override fun visitFunction(node: Node.Function): List<IntermediateToken> {
        if (node.isNative) return emptyList()

        var iTokens = mutableListOf<IntermediateToken>()

        iTokens += JMP.intermediate
        iTokens += IntermediateToken.AwaitEnd()

        pos += 2

        val start = pos.toFloat()

        functions[node.id] = pos

        iTokens += FRAME.intermediate
        iTokens += node.offset.toFloat().intermediate

        pos += 2

        for (param in node.params) {
            iTokens += when (param.dataType) {
                is DataType.Array -> ASTORE.intermediate

                else              -> STORE.intermediate
            }
            iTokens += param.address.toFloat().intermediate

            pos += 2
        }

        for (stmt in node.body) {
            iTokens += visit(stmt)
        }

        if (iTokens.none { it is IntermediateToken.Ok && it.token == RET }) {
            iTokens += PUSH.intermediate
            iTokens += 0F.intermediate
            iTokens += RET.intermediate

            pos += 3
        }

        val end = pos.toFloat()

        iTokens = resolveStartAndEnd(iTokens, start, end)

        return iTokens
    }

    override fun visitReturn(node: Node.Return): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        val subNode = node.node

        if (subNode != null) {
            iTokens += visit(subNode)
        }

        iTokens += RET.intermediate

        pos++

        return iTokens
    }

    override fun visitExpression(node: Node.Expression): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        iTokens += visit(node.node)

        iTokens += POP.intermediate

        pos++

        return iTokens
    }

    override fun visitValue(node: Node.Value): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        iTokens += PUSH.intermediate
        iTokens += node.value.value.intermediate

        pos += 2

        return iTokens
    }

    override fun visitString(node: Node.String): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        for (c in node.value.value.reversed()) {
            iTokens += PUSH.intermediate
            iTokens += c.code.toFloat().intermediate

            pos += 2
        }

        iTokens += PUSH.intermediate
        iTokens += node.value.value.length.toFloat().intermediate

        pos += 2

        return iTokens
    }

    override fun visitName(node: Node.Name): List<IntermediateToken> {
        error("Should not visit Name!")
    }

    override fun visitVariable(node: Node.Variable): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        if (node.isGlobal) {
            iTokens += GLOBAL.intermediate

            pos++
        }

        iTokens += (if (node.dataType is DataType.Array) ALOAD else LOAD).intermediate
        iTokens += node.address.toFloat().intermediate

        pos += 2

        return iTokens
    }

    override fun visitType(node: Node.Type): List<IntermediateToken> {
        error("Should not visit Type!")
    }

    override fun visitArray(node: Node.Array): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        for (element in node.elements.reversed()) {
            iTokens += visit(element)
        }

        iTokens += PUSH.intermediate
        iTokens += Bytecode.Value(node.dataType.offset.toFloat() - 1).intermediate

        pos += 2

        return iTokens
    }

    override fun visitUnary(node: Node.Unary): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        iTokens += visit(node.operand)

        iTokens += node.operator.instruction.intermediate

        pos++

        return iTokens
    }

    override fun visitSize(node: Node.Size): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        if (node.variable.dataType is DataType.Array) {
            iTokens += SIZE.intermediate
            iTokens += node.variable.address.toFloat().intermediate
        }
        else {
            iTokens += PUSH.intermediate
            iTokens += 1F.intermediate
        }

        pos += 2

        return iTokens
    }

    override fun visitBinary(node: Node.Binary): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        iTokens += visit(node.operandLeft)
        iTokens += visit(node.operandRight)

        val additional = node.operator.instructions.map { it.intermediate }
        iTokens += additional

        pos += additional.size

        return iTokens
    }

    override fun visitAssign(node: Node.Assign): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        iTokens += visit(node.node)

        iTokens += DUP.intermediate
        iTokens += STORE.intermediate
        iTokens += node.variable.address.toFloat().intermediate

        pos += 3

        return iTokens
    }

    override fun visitInvoke(node: Node.Invoke): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        for (arg in node.args.reversed()) {
            iTokens += visit(arg)
        }

        val address = functions[node.id]!!

        iTokens += CALL.intermediate
        iTokens += address.toFloat().intermediate

        pos += 2

        return iTokens
    }

    override fun visitSystemCall(node: Node.SystemCall): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        for (arg in node.args.reversed()) {
            iTokens += visit(arg)
        }

        iTokens += SYS.intermediate
        iTokens += node.id.toFloat().intermediate

        pos += 2

        return iTokens
    }

    override fun visitGetIndex(node: Node.GetIndex): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        val origin = node.variable.address

        val indices = node
            .indices
            .reversed()
            .map { visit(it) }

        for (index in indices) {
            iTokens += index
        }

        if (node.variable.isGlobal) {
            iTokens += GLOBAL.intermediate

            pos++
        }

        iTokens += (if (node.indices.size < node.arrayType.dimension) IALOAD else ILOAD).intermediate
        iTokens += origin.toFloat().intermediate
        iTokens += indices.size.toFloat().intermediate
        pos += 3

        return iTokens
    }

    override fun visitSetIndex(node: Node.SetIndex): List<IntermediateToken> {
        val iTokens = mutableListOf<IntermediateToken>()

        val origin = node.variable.address

        val indices = node
            .indices
            .reversed()
            .map { visit(it) }

        iTokens += visit(node.value)

        for (index in indices) {
            iTokens += index
        }

        iTokens += if (node.indices.size < node.arrayType.dimension) {
            IASTORE
        }
        else {
            ISTORE
        }.intermediate
        iTokens += origin.toFloat().intermediate
        iTokens += indices.size.toFloat().intermediate
        iTokens += PUSH.intermediate
        iTokens += 0F.intermediate
        pos += 5

        return iTokens
    }
}