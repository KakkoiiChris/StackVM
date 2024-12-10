package kakkoiichris.stackvm.lang.compiler

import kakkoiichris.stackvm.lang.compiler.Bytecode.Instruction.*
import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.lang.parser.Node
import java.util.*

class Compiler(
    private val program: Node.Program,
    private val optimize: Boolean,
    private val generateComments: Boolean
) : Node.Visitor<List<Token>> {
    private var pos = 0

    private val functions = mutableMapOf<Int, Int>()

    private val offsetStack = Stack<Int>()

    private val memoryToFree = Stack<MutableList<Int>>()

    fun compile() =
        convert()
            .map { it.value }
            .toDoubleArray()

    fun convert(): List<Bytecode> {
        val tokens = visit(program).toMutableList()

        tokens.optimize()

        val subTokens = tokens.filterIsInstance<Token.Ok>()

        if (tokens.size > subTokens.size) error("Unresolved intermediate token!")

        val bytecodes = subTokens
            .map { it.bytecode }
            .toMutableList()
            .apply { add(HALT) }

        return bytecodes
    }

    private fun MutableList<Token>.optimize() {
        if (optimize) {
            var i = 0

            while (i < lastIndex) {
                val ta = get(1)
                val tb = get(i + 1)

                if (ta !is Token.Ok || tb !is Token.Ok) {
                    i++

                    continue
                }

                val a = ta.bytecode
                val b = tb.bytecode

                when {
                    a === NOT && b === NOT -> repeat(2) { removeAt(i) }

                    a === NEG && b === NEG -> repeat(2) { removeAt(i) }

                    a === ADD && b === NEG -> {
                        repeat(2) { removeAt(i) }

                        add(i, SUB.ok)
                    }

                    a === SUB && b === NEG -> {
                        repeat(2) { removeAt(i) }

                        add(i, ADD.ok)
                    }

                    else                   -> i++
                }
            }
        }
    }

    private operator fun MutableList<Token>.plusAssign(x: Bytecode) {
        if (x is Bytecode.Comment && !generateComments) return

        add(x.ok)

        if (x !is Bytecode.Comment) pos++
    }

    private operator fun MutableList<Token>.plusAssign(x: Token) {
        add(x)

        pos++
    }

    private operator fun MutableList<Token>.plusAssign(x: Double) {
        add(x.ok)

        pos++
    }

    private operator fun MutableList<Token>.plusAssign(x: Int) {
        add(x.toDouble().ok)

        pos++
    }

    private val Double.ok get() = Token.Ok(Bytecode.Value(this))

    private fun push() {
        memoryToFree.push(mutableListOf())
    }

    private fun pop() {
        memoryToFree.pop()
    }

    private fun addMemory(id: Int) {
        memoryToFree.peek().add(id)
    }

    private fun freeMemory(): List<Token> {
        val tokens = mutableListOf<Token>()

        val free = memoryToFree.peek()

        for (id in free) {
            tokens += FREE
            tokens += id
        }

        return tokens
    }

    private fun resolveStartAndEnd(tokens: List<Token>, start: Double, end: Double) =
        tokens
            .map { it.resolveStartAndEnd(start, end) ?: it }
            .toMutableList()

    private fun resolveLabelStartAndEnd(tokens: List<Token>, label: Node.Name, start: Double, end: Double) =
        tokens
            .map { it.resolveLabelStartAndEnd(label, start, end) ?: it }
            .toMutableList()

    private fun resolveLast(tokens: List<Token>, last: Double) =
        tokens
            .map { it.resolveLast(last) ?: it }
            .toMutableList()

    override fun visitProgram(node: Node.Program): List<Token> {
        val tokens = mutableListOf<Token>()

        tokens += JMP
        tokens += Double.NaN

        offsetStack.push(node.offset)

        push()

        for (statement in node.statements) {
            tokens += visit(statement)
        }

        val mainReturnPos = pos

        tokens += visit(program.mainReturn)

        tokens[1] = mainReturnPos.toDouble().ok

        tokens += freeMemory()

        pop()

        offsetStack.pop()

        return tokens
    }

    override fun visitDeclareSingle(node: Node.DeclareSingle): List<Token> {
        val tokens = mutableListOf<Token>()

        if (node.node != null) {
            tokens += visit(node.node)
        }

        tokens += STO
        tokens += node.address

        return tokens
    }

    override fun visitDeclareArray(node: Node.DeclareArray): List<Token> {
        val tokens = mutableListOf<Token>()

        if (node.node != null) {
            tokens += visit(node.node)
        }
        else if (!node.variable.dataType.isHeapAllocated) {
            val sizes = (node.variable.dataType as DataType.Array).sizes

            tokens += getDefaultArray(*sizes)
        }
        else {
            tokens += getDefaultArray(1)
        }

        if (node.variable.dataType.isHeapAllocated) {
            tokens += ALLOC
            tokens += node.id
            tokens += HEAP
            tokens += ASTO
            tokens += node.id

            addMemory(node.id)
        }
        else {
            tokens += ASTO
            tokens += node.address
        }

        return tokens
    }

    private fun getDefaultArray(vararg sizes: Int) =
        getSubArray(sizes[0], sizes.drop(1).toIntArray())

    private fun getSubArray(size: Int, rest: IntArray): List<Token> {
        val tokens = mutableListOf<Token>()

        if (rest.isEmpty()) {
            repeat(size) {
                tokens += PUSH
                tokens += 0.0
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

        var last = -1.0

        for ((i, branch) in node.branches.withIndex()) {
            val (_, condition, body) = branch

            val start = pos.toDouble()

            if (condition != null) {
                tokens += visit(condition)

                tokens += NOT
                tokens += JIF
                tokens += Token.AwaitEnd()
            }

            push()

            for (stmt in body) {
                tokens += visit(stmt)
            }

            tokens += freeMemory()

            pop()

            if (i != node.branches.lastIndex) {
                tokens += JMP
                tokens += Token.AwaitLast()
            }

            tokens.optimize()

            val end = pos.toDouble()
            last = end

            tokens = resolveStartAndEnd(tokens, start, end)
        }

        tokens = resolveLast(tokens, last)

        return tokens
    }

    override fun visitWhile(node: Node.While): List<Token> {
        var tokens = mutableListOf<Token>()

        val start = pos.toDouble()

        tokens += visit(node.condition)

        tokens += NOT
        tokens += JIF
        tokens += Token.AwaitEnd()

        push()

        for (stmt in node.body) {
            tokens += visit(stmt)
        }

        tokens += freeMemory()

        pop()

        tokens += JMP
        tokens += Token.AwaitStart()

        tokens.optimize()

        val end = pos.toDouble()

        tokens = resolveStartAndEnd(tokens, start, end)

        val label = node.label

        if (label != null) {
            tokens = resolveLabelStartAndEnd(tokens, label, start, end)
        }

        return tokens
    }

    override fun visitDo(node: Node.Do): List<Token> {
        var tokens = mutableListOf<Token>()

        val start = pos.toDouble()

        push()

        for (stmt in node.body) {
            tokens += visit(stmt)
        }

        tokens += freeMemory()

        pop()

        tokens += visit(node.condition)

        tokens += JIF
        tokens += Token.AwaitStart()

        tokens.optimize()

        val end = pos.toDouble()

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

        val start = pos.toDouble()

        if (node.condition != null) {
            tokens += visit(node.condition)

            tokens += NOT
            tokens += JIF
            tokens += Token.AwaitEnd()
        }

        push()

        for (stmt in node.body) {
            tokens += visit(stmt)
        }

        tokens += freeMemory()

        pop()

        if (node.increment != null) {
            tokens += visit(node.increment)

            tokens += POP
        }

        tokens += JMP
        tokens += Token.AwaitStart()

        tokens.optimize()

        val end = pos.toDouble()

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

        tokens += Bytecode.Comment("${node.signature} @ ${node.location}")

        val start = pos.toDouble()

        functions[node.id] = pos

        offsetStack.push(node.offset)

        push()

        for (param in node.params) {
            if (param.dataType.isHeapAllocated) {
                tokens += ALLOC
                tokens += param.id
                tokens += HEAP
                tokens += ASTO
                tokens += param.id
            }
            else {
                tokens += if (param.dataType is DataType.Array) ASTO else STO
                tokens += param.address
            }
        }

        for (stmt in node.body) {
            tokens += visit(stmt)
        }

        if (tokens.none { it is Token.Ok && it.bytecode == RET }) {
            tokens += freeMemory()
            tokens += PUSH
            tokens += 0.0
            tokens += RET
        }

        pop()

        offsetStack.pop()

        val end = pos.toDouble()

        tokens = resolveStartAndEnd(tokens, start, end)

        return tokens
    }

    override fun visitReturn(node: Node.Return): List<Token> {
        val tokens = mutableListOf<Token>()

        val subNode = node.node

        if (subNode != null) {
            tokens += visit(subNode)
        }

        tokens += freeMemory()

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

        for (c in node.value.reversed()) {
            tokens += PUSH
            tokens += c.code
        }

        tokens += PUSH
        tokens += node.value.length

        return tokens
    }

    override fun visitVariable(node: Node.Variable): List<Token> {
        val tokens = mutableListOf<Token>()

        if (node.dataType.isHeapAllocated) {
            tokens += HEAP
            tokens += ALOD
            tokens += node.id
        }
        else {
            if (node.isGlobal) {
                tokens += GLOB
            }

            tokens += if (DataType.isArray(node.dataType)) ALOD else LOD
            tokens += node.address
        }

        return tokens
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

        if (DataType.isArray(node.variable.dataType)) {
            val instruction = if (node.arrayType.dimension > 1) ASIZE else SIZE

            if (node.variable.dataType.isHeapAllocated) {
                tokens += HEAP
                tokens += instruction
                tokens += node.variable.id
            }
            else {
                tokens += instruction
                tokens += node.variable.address
            }
        }
        else {
            tokens += PUSH
            tokens += 1.0
        }

        return tokens
    }

    override fun visitIndexSize(node: Node.IndexSize): List<Token> {
        val tokens = mutableListOf<Token>()

        for (index in node.indices) {
            tokens += visit(index)
        }

        val instruction = if (node.indices.size < node.arrayType.dimension - 1) IASIZE else ISIZE

        if (node.variable.dataType.isHeapAllocated) {
            tokens += HEAP
            tokens += instruction
            tokens += node.variable.id
            tokens += node.indices.size
        }
        else {
            tokens += instruction
            tokens += node.variable.address
            tokens += node.indices.size
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

        if (node.variable.dataType.isHeapAllocated) {
            tokens += REALLOC
            tokens += node.variable.id
            tokens += HEAP
            tokens += ASTO
            tokens += node.variable.id
        }
        else if (DataType.isArray(node.dataType)) {
            tokens += ASTO
            tokens += node.variable.address
        }
        else {
            tokens += DUP
            tokens += STO
            tokens += node.variable.address
        }

        return tokens
    }

    override fun visitInvoke(node: Node.Invoke): List<Token> {
        val tokens = mutableListOf<Token>()

        for (arg in node.args) {
            tokens += visit(arg)
        }

        val offset = offsetStack.peek()!!

        tokens += FRAME
        tokens += offset + node.offset

        val address = functions[node.id]!!

        tokens += CALL
        tokens += address

        return tokens
    }

    override fun visitSystemCall(node: Node.SystemCall): List<Token> {
        val tokens = mutableListOf<Token>()

        tokens += ARG

        for (arg in node.args) {
            tokens += visit(arg)
        }

        tokens += SYS
        tokens += node.id

        return tokens
    }

    override fun visitGetIndex(node: Node.GetIndex): List<Token> {
        val tokens = mutableListOf<Token>()

        val indices = node
            .indices
            .reversed()
            .map { visit(it) }

        for (index in indices) {
            tokens += index
        }

        val instruction = if (node.indices.size < node.arrayType.dimension) IALOD else ILOD

        if (node.variable.dataType.isHeapAllocated) {
            tokens += HEAP
            tokens += instruction
            tokens += node.variable.id
        }
        else {
            if (node.variable.isGlobal) {
                tokens += GLOB
            }

            tokens += instruction
            tokens += node.variable.address
        }

        tokens += indices.size

        return tokens
    }

    override fun visitSetIndex(node: Node.SetIndex): List<Token> {
        val tokens = mutableListOf<Token>()

        val indices = node
            .indices
            .reversed()
            .map { visit(it) }

        tokens += visit(node.value)

        for (index in indices) {
            tokens += index
        }

        val instruction = if (node.indices.size < node.arrayType.dimension) IASTO else ISTO

        if (node.variable.dataType.isHeapAllocated) {
            tokens += HEAP
            tokens += instruction
            tokens += node.variable.id
        }
        else {
            tokens += instruction
            tokens += node.variable.address
        }

        tokens += indices.size
        tokens += PUSH
        tokens += 0.0

        return tokens
    }
}