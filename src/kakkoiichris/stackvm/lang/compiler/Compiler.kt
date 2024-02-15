package kakkoiichris.stackvm.lang.compiler

import kakkoiichris.stackvm.lang.compiler.Bytecode.Instruction.*
import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.lang.parser.Node

class Compiler(
    private val program: Node.Program,
    private val optimize: Boolean
) : Node.Visitor<List<Token>> {
    private var pos = 0

    private val functions = mutableMapOf<Int, Int>()

    fun compile() =
        convert()
            .map { it.value }
            .toFloatArray()

    fun convert(): List<Bytecode> {
        val tokens = visit(program).toMutableList()

        val subTokens = tokens.filterIsInstance<Token.Ok>()

        if (tokens.size > subTokens.size) error("Unresolved intermediate token!")

        val bytecodes = subTokens
            .map { it.token }
            .toMutableList()
            .apply { add(HALT) }

        if (optimize) {
            var i = 0

            while (i < bytecodes.lastIndex) {
                val a = bytecodes[i]
                val b = bytecodes[i + 1]

                if (a === b && a in listOf(NOT, NEG)) {
                    repeat(2) { bytecodes.removeAt(i) }
                }
                else {
                    i++
                }
            }
        }

        return bytecodes
    }

    private operator fun MutableList<Token>.plusAssign(x: Bytecode) {
        add(x.intermediate)

        pos++
    }

    private operator fun MutableList<Token>.plusAssign(x: Float) {
        add(x.intermediate)

        pos++
    }

    private operator fun MutableList<Token>.plusAssign(x: Int) {
        add(x.toFloat().intermediate)

        pos++
    }

    private fun resolveStartAndEnd(tokens: List<Token>, start: Float, end: Float) =
        tokens
            .map { it.resolveStartAndEnd(start, end) ?: it }
            .toMutableList()

    private fun resolveLabelStartAndEnd(tokens: List<Token>, label: Node.Name, start: Float, end: Float) =
        tokens
            .map { it.resolveLabelStartAndEnd(label, start, end) ?: it }
            .toMutableList()

    private fun resolveLast(tokens: List<Token>, last: Float) =
        tokens
            .map { it.resolveLast(last) ?: it }
            .toMutableList()

    private val Float.intermediate get() = Token.Ok(Bytecode.Value(this))

    override fun visitProgram(node: Node.Program): List<Token> {
        val tokens = mutableListOf<Token>()

        for (statement in node.statements) {
            tokens += visit(statement)
        }

        return tokens
    }

    override fun visitDeclareSingle(node: Node.DeclareSingle): List<Token> {
        val tokens = mutableListOf<Token>()

        if (node.node != null) {
            tokens += visit(node.node)
        }

        tokens += STORE
        tokens += node.address.toFloat()

        return tokens
    }

    override fun visitDeclareArray(node: Node.DeclareArray): List<Token> {
        val tokens = mutableListOf<Token>()

        if (node.node != null) {
            tokens += visit(node.node)
        }
        else {
            val sizes = (node.variable.dataType as DataType.Array).sizes

            tokens += getDefaultArray(sizes)
        }

        tokens += ASTORE
        tokens += node.address.toFloat()

        return tokens
    }

    private fun getDefaultArray(sizes: IntArray) =
        getSubArray(sizes[0], sizes.drop(1).toIntArray())

    private fun getSubArray(size: Int, rest: IntArray): List<Token> {
        val tokens = mutableListOf<Token>()

        if (rest.isEmpty()) {
            repeat(size) {
                tokens += PUSH
                tokens += 0F
            }

            tokens += PUSH
            tokens += size
        }
        else {
            var totalSize = 0

            repeat(size) {
                val subArray = getSubArray(rest[0], rest.drop(1).toIntArray())

                totalSize += subArray.size / 2

                tokens += subArray
            }

            tokens += PUSH
            tokens += totalSize
        }

        return tokens
    }

    override fun visitIf(node: Node.If): List<Token> {
        var tokens = mutableListOf<Token>()

        var last = -1F

        for ((i, branch) in node.branches.withIndex()) {
            val (_, condition, body) = branch

            val start = pos.toFloat()

            if (condition != null) {
                tokens += visit(condition)

                tokens += NOT
                tokens += JIF
                tokens += Token.AwaitEnd()
            }

            for (stmt in body) {
                tokens += visit(stmt)
            }

            if (i != node.branches.lastIndex) {
                tokens += JMP
                tokens += Token.AwaitLast()
            }

            val end = pos.toFloat()
            last = end

            tokens = resolveStartAndEnd(tokens, start, end)
        }

        tokens = resolveLast(tokens, last)

        return tokens
    }

    override fun visitWhile(node: Node.While): List<Token> {
        var tokens = mutableListOf<Token>()

        val start = pos.toFloat()

        tokens += visit(node.condition)

        tokens += NOT
        tokens += JIF
        tokens += Token.AwaitEnd()

        for (stmt in node.body) {
            tokens += visit(stmt)
        }

        tokens += JMP
        tokens += Token.AwaitStart()

        val end = pos.toFloat()

        tokens = resolveStartAndEnd(tokens, start, end)

        val label = node.label

        if (label != null) {
            tokens = resolveLabelStartAndEnd(tokens, label, start, end)
        }

        return tokens
    }

    override fun visitDo(node: Node.Do): List<Token> {
        var tokens = mutableListOf<Token>()

        val start = pos.toFloat()

        for (stmt in node.body) {
            tokens += visit(stmt)
        }

        tokens += visit(node.condition)

        tokens += JIF
        tokens += Token.AwaitStart()

        val end = pos.toFloat()

        tokens = resolveStartAndEnd(tokens, start, end)

        val label = node.label

        if (label != null) {
            tokens = resolveLabelStartAndEnd(tokens, label, start, end)
        }

        return tokens
    }

    override fun visitFor(node: Node.For): List<Token> {
        var tokens = mutableListOf<Token>()

        if (node.init != null) {
            tokens += visit(node.init)
        }

        val start = pos.toFloat()

        if (node.condition != null) {
            tokens += visit(node.condition)

            tokens += NOT
            tokens += JIF
            tokens += Token.AwaitEnd()
        }

        for (stmt in node.body) {
            tokens += visit(stmt)
        }

        if (node.increment != null) {
            tokens += visit(node.increment)

            tokens += POP
        }

        tokens += JMP
        tokens += Token.AwaitStart()

        val end = pos.toFloat()

        tokens = resolveStartAndEnd(tokens, start, end)

        if (node.label != null) {
            tokens = resolveLabelStartAndEnd(tokens, node.label, start, end)
        }

        return tokens
    }

    override fun visitBreak(node: Node.Break): List<Token> {
        val label = node.label

        val tokens = mutableListOf<Token>()

        tokens += JMP
        tokens += if (label != null) Token.AwaitLabelEnd(label) else Token.AwaitEnd()

        return tokens
    }

    override fun visitContinue(node: Node.Continue): List<Token> {
        val label = node.label

        val tokens = mutableListOf<Token>()

        tokens += JMP
        tokens += if (label != null) Token.AwaitLabelStart(label) else Token.AwaitStart()

        return tokens
    }

    override fun visitFunction(node: Node.Function): List<Token> {
        if (node.isNative) return emptyList()

        var tokens = mutableListOf<Token>()

        tokens += JMP
        tokens += Token.AwaitEnd()

        val start = pos.toFloat()

        functions[node.id] = pos

        tokens += FRAME
        tokens += node.offset

        for (param in node.params) {
            tokens += if (param.dataType is DataType.Array) ASTORE else STORE
            tokens += param.address
        }

        for (stmt in node.body) {
            tokens += visit(stmt)
        }

        if (tokens.none { it is Token.Ok && it.token == RET }) {
            tokens += PUSH
            tokens += 0F
            tokens += RET
        }

        val end = pos.toFloat()

        tokens = resolveStartAndEnd(tokens, start, end)

        return tokens
    }

    override fun visitReturn(node: Node.Return): List<Token> {
        val tokens = mutableListOf<Token>()

        val subNode = node.node

        if (subNode != null) {
            tokens += visit(subNode)
        }

        tokens += RET

        return tokens
    }

    override fun visitExpression(node: Node.Expression): List<Token> {
        val tokens = mutableListOf<Token>()

        tokens += visit(node.node)

        tokens += POP

        return tokens
    }

    override fun visitValue(node: Node.Value): List<Token> {
        val tokens = mutableListOf<Token>()

        tokens += PUSH
        tokens += node.value.value

        return tokens
    }

    override fun visitString(node: Node.String): List<Token> {
        val tokens = mutableListOf<Token>()

        for (c in node.value.value.reversed()) {
            tokens += PUSH
            tokens += c.code
        }

        tokens += PUSH
        tokens += node.value.value.length

        return tokens
    }

    override fun visitName(node: Node.Name): List<Token> {
        error("Should not visit Name!")
    }

    override fun visitVariable(node: Node.Variable): List<Token> {
        val tokens = mutableListOf<Token>()

        if (node.isGlobal) {
            tokens += GLOBAL
        }

        tokens += if (node.dataType is DataType.Array) ALOAD else LOAD
        tokens += node.address

        return tokens
    }

    override fun visitType(node: Node.Type): List<Token> {
        error("Should not visit Type!")
    }

    override fun visitArray(node: Node.Array): List<Token> {
        val tokens = mutableListOf<Token>()

        for (element in node.elements.reversed()) {
            tokens += visit(element)
        }

        tokens += PUSH
        tokens += node.dataType.offset - 1

        return tokens
    }

    override fun visitUnary(node: Node.Unary): List<Token> {
        val tokens = mutableListOf<Token>()

        tokens += visit(node.operand)

        tokens += node.operator.instruction

        return tokens
    }

    override fun visitSize(node: Node.Size): List<Token> {
        val tokens = mutableListOf<Token>()

        if (node.variable.dataType is DataType.Array) {
            tokens += SIZE
            tokens += node.variable.address
        }
        else {
            tokens += PUSH
            tokens += 1F
        }

        return tokens
    }

    override fun visitBinary(node: Node.Binary): List<Token> {
        val tokens = mutableListOf<Token>()

        tokens += visit(node.operandLeft)
        tokens += visit(node.operandRight)

        node.operator.instructions
            .forEach { tokens += it }

        return tokens
    }

    override fun visitAssign(node: Node.Assign): List<Token> {
        val tokens = mutableListOf<Token>()

        tokens += visit(node.node)

        tokens += DUP
        tokens += STORE
        tokens += node.variable.address

        return tokens
    }

    override fun visitInvoke(node: Node.Invoke): List<Token> {
        val tokens = mutableListOf<Token>()

        for (arg in node.args.reversed()) {
            tokens += visit(arg)
        }

        val address = functions[node.id]!!

        tokens += CALL
        tokens += address

        return tokens
    }

    override fun visitSystemCall(node: Node.SystemCall): List<Token> {
        val tokens = mutableListOf<Token>()

        for (arg in node.args.reversed()) {
            tokens += visit(arg)
        }

        tokens += SYS
        tokens += node.id

        return tokens
    }

    override fun visitGetIndex(node: Node.GetIndex): List<Token> {
        val tokens = mutableListOf<Token>()

        val origin = node.variable.address

        val indices = node
            .indices
            .reversed()
            .map { visit(it) }

        for (index in indices) {
            tokens += index
        }

        if (node.variable.isGlobal) {
            tokens += GLOBAL
        }

        tokens += if (node.indices.size < node.arrayType.dimension) IALOAD else ILOAD
        tokens += origin
        tokens += indices.size

        return tokens
    }

    override fun visitSetIndex(node: Node.SetIndex): List<Token> {
        val tokens = mutableListOf<Token>()

        val origin = node.variable.address

        val indices = node
            .indices
            .reversed()
            .map { visit(it) }

        tokens += visit(node.value)

        for (index in indices) {
            tokens += index
        }

        tokens += if (node.indices.size < node.arrayType.dimension) IASTORE else ISTORE
        tokens += origin
        tokens += indices.size
        tokens += PUSH
        tokens += 0F

        return tokens
    }
}