/*   ______  ____   ____  ____    ____  _____
 * .' ____ \|_  _| |_  _||_   \  /   _||_   _|
 * | (___ \_| \ \   / /    |   \/   |    | |
 *  _.____`.   \ \ / /     | |\  /| |    | |   _
 * | \____) |   \ ' /     _| |_\/_| |_  _| |__/ |
 *  \______.'    \_/     |_____||_____||________|
 *
 *         Stack Virtual Machine Language
 *     Copyright (C) 2024 Christian Alexander
 */
package kakkoiichris.svml.lang.compiler

import kakkoiichris.svml.lang.compiler.Bytecode.Instruction.*
import kakkoiichris.svml.lang.parser.DataType
import kakkoiichris.svml.lang.parser.Node
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

    private fun resolveStartAndEnd(tokens: List<Token>, start: Double = 0.0, end: Double = 0.0) =
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

    private fun resolveFunction(tokens: List<Token>, id: Int) =
        tokens
            .map { it.resolveFunction(id, functions[id]!!.toDouble()) ?: it }
            .toMutableList()

    override fun visitProgram(node: Node.Program): List<Token> {
        var tokens = mutableListOf<Token>()

        tokens += JMP
        tokens += Double.NaN

        offsetStack.push(node.offset)

        push()

        for (statement in node.subNodes) {
            tokens += visit(statement)
        }

        tokens[1] = pos.toDouble().ok

        tokens += visit(program.mainReturn)

        for (function in node.functions.filter { !it.isNative }) {
            tokens = resolveFunction(tokens, function.id)
        }

        tokens += freeMemory()

        pop()

        offsetStack.pop()

        return tokens
    }

    override fun visitDeclare(node: Node.Declare): List<Token> {
        val tokens = mutableListOf<Token>()

        if (DataType.isArray(node.dataType, node.context.source)) {
            if (node.assigned != null) {
                tokens += visit(node.assigned)
            }
            else if (!node.name.dataType.isHeapAllocated(node.name.context.source)) {
                val sizes = (node.name.dataType as DataType.Array).sizes

                tokens += getDefaultArray(*sizes)
            }
            else {
                tokens += getDefaultArray(1)
            }

            if (node.name.dataType.isHeapAllocated(node.name.context.source)) {
                tokens += ALLOC
                tokens += node.id
                tokens += PUSH
                tokens += node.id
                tokens += PUSH
                tokens += 0
                tokens += HASTO

                addMemory(node.id)
            }
            else {
                tokens += PUSH
                tokens += node.address
                tokens += ASTO
            }
        }
        else {
            if (node.assigned != null) {
                tokens += visit(node.assigned)
            }

            tokens += PUSH
            tokens += node.address
            tokens += STO
        }

        return tokens
    }

    private fun getDefaultArray(vararg dimensions: Int) =
        getSubArray(dimensions, 0)

    private fun getSubArray(dimensions: IntArray, i: Int): List<Token> {
        val tokens = mutableListOf<Token>()

        if (i == dimensions.lastIndex) {
            val dimension = dimensions[i]

            tokens += PUSH
            tokens += 0.0

            repeat(dimension - 1) {
                tokens += DUP
            }

            tokens += PUSH
            tokens += dimension
        }
        else {
            val dimension = dimensions[i]

            var totalSize = 0

            repeat(dimension) {
                val subArray = getSubArray(dimensions, i + 1)

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

        tokens += Bytecode.Comment("${node.signature} @ ${node.context.getCodeStamp()}")

        val start = pos.toDouble()

        functions[node.id] = pos

        offsetStack.push(node.offset)

        push()

        for (param in node.params) {
            if (param.dataType.isHeapAllocated(param.context.source)) {
                tokens += ALLOC
                tokens += param.id
                tokens += PUSH
                tokens += param.id
                tokens += PUSH
                tokens += 0
                tokens += HASTO
            }
            else {
                tokens += PUSH
                tokens += param.address
                tokens += if (param.dataType is DataType.Array) ASTO else STO
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

        val subNode = node.value

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

        if (node.node !is Node.SetIndex) {
            tokens += POP
        }

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

    override fun visitName(node: Node.Name): List<Token> {
        val tokens = mutableListOf<Token>()

        if (node.dataType.isHeapAllocated(node.context.source)) {
            tokens += PUSH
            tokens += node.id
            tokens += HALOD
        }
        else {
            tokens += PUSH
            tokens += node.address

            if (node.isGlobal) {
                tokens += GLOB
            }

            tokens += if (DataType.isArray(node.dataType, node.context.source)) ALOD else LOD
        }

        return tokens
    }

    override fun visitArray(node: Node.Array): List<Token> {
        val tokens = mutableListOf<Token>()

        for (element in node.elements.reversed()) {
            tokens += visit(element)
        }

        tokens += PUSH
        tokens += node.dataType.getOffset(node.context.source) - 1

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

        val isArray = DataType.isArray(node.name.dataType, node.name.context.source)

        if (isArray) {
            val arrayType = node.name.getArrayType()

            val isHeap = arrayType.isHeapAllocated(node.name.context.source)

            val instruction = if (arrayType.dimension > 1) {
                if (isHeap) HASIZE else ASIZE
            }
            else {
                if (isHeap) HSIZE else SIZE
            }

            val location = if (isHeap) node.name.id else node.name.address

            tokens += PUSH
            tokens += location

            if (isHeap) {
                tokens += PUSH
                tokens += 0
            }

            tokens += instruction
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

        val instruction =
            PUSH//TODO if (node.indices.size < node.getArrayType(node.variable.context.source).dimension - 1) IASIZE else ISIZE

        if (node.name.dataType.isHeapAllocated(node.name.context.source)) {
            //TODO tokens += HEAP
            tokens += instruction
            tokens += node.name.id
            tokens += node.indices.size
        }
        else {
            tokens += instruction
            tokens += node.name.address
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

    override fun visitLogical(node: Node.Logical): List<Token> {
        var tokens = mutableListOf<Token>()

        tokens += visit(node.operandLeft)

        if (node.operator == Node.Logical.Operator.AND) {
            tokens += NOT
        }

        tokens += DUP
        tokens += JIF
        tokens += Token.AwaitEnd()

        tokens += visit(node.operandRight)

        tokens += node.operator.instruction

        tokens = resolveStartAndEnd(tokens, end = pos.toDouble())

        return tokens
    }

    override fun visitAssign(node: Node.Assign): List<Token> {
        val tokens = mutableListOf<Token>()

        tokens += visit(node.assigned)

        val isHeap = node.name.dataType.isHeapAllocated(node.name.context.source)

        val isArray = DataType.isArray(
            node.name.dataType,
            node.name.context.source
        )

        if (isHeap) {
            tokens += REALLOC
            tokens += node.name.id
            tokens += PUSH
            tokens += node.name.id
            tokens += PUSH
            tokens += 0
            tokens += HASTO
        }
        else if (isArray) {
            tokens += PUSH
            tokens += node.name.address
            tokens += ASTO
        }
        else {
            tokens += DUP
            tokens += PUSH
            tokens += node.name.address
            tokens += STO
        }

        return tokens
    }

    override fun visitInvoke(node: Node.Invoke): List<Token> {
        val tokens = mutableListOf<Token>()

        if (node.isNative) {
            tokens += ARG

            for (arg in node.args) {
                tokens += visit(arg)
            }

            tokens += SYS
            tokens += node.id
        }
        else {
            for (arg in node.args) {
                tokens += visit(arg)
            }

            val offset = offsetStack.peek()!!

            tokens += FRAME
            tokens += offset + node.offset

            //val address = functions[node.id] ?: svmlError("Function id '${node.id}' not found", node.context.source, node.context)
            val address = Token.AwaitFunction(node.id, offset)

            tokens += CALL
            tokens += address
        }

        return tokens
    }

    override fun visitGetIndex(node: Node.GetIndex): List<Token> {
        val tokens = mutableListOf<Token>()

        val indices = node.indices

        val arrayType = node.name.getArrayType()

        val dimension = arrayType.dimension

        val sizes = arrayType.sizes

        val isHeap = node.name.dataType.isHeapAllocated(node.name.context.source)

        val instruction = if (node.indices.size < dimension) {
            if (isHeap) HALOD else ALOD
        }
        else {
            if (isHeap) HLOD else LOD
        }

        val location = if (isHeap) node.name.id else node.name.address

        tokens += PUSH
        tokens += location

        if (isHeap) {
            tokens += PUSH
            tokens += 0
        }

        for ((i, index) in indices.dropLast(1).withIndex()) {
            tokens += INC
            tokens += PUSH
            tokens += sizes[i]
            tokens += INC
            tokens += visit(index)
            tokens += MUL
            tokens += ADD
        }

        if (indices.size == dimension) {
            tokens += INC
            tokens += visit(indices.last())
            tokens += ADD
        }

        if (node.name.isGlobal) {
            tokens += GLOB
        }

        tokens += instruction

        return tokens
    }

    override fun visitSetIndex(node: Node.SetIndex): List<Token> {
        val tokens = mutableListOf<Token>()

        val indices = node.indices

        val arrayType = node.getArrayType()

        val dimension = arrayType.dimension

        val sizes = arrayType.sizes

        val isHeap = node.name.dataType.isHeapAllocated(node.name.context.source)

        val instruction = if (node.indices.size < dimension) {
            if (isHeap) HALOD else ALOD
        }
        else {
            if (isHeap) HLOD else LOD
        }

        val location = if (isHeap) node.name.id else node.name.address

        tokens += visit(node.value)

        tokens += PUSH
        tokens += location

        if (isHeap) {
            tokens += PUSH
            tokens += 0
        }

        for ((i, index) in indices.dropLast(1).withIndex()) {
            tokens += INC
            tokens += PUSH
            tokens += sizes[i]
            tokens += INC
            tokens += visit(index)
            tokens += MUL
            tokens += ADD
        }

        if (indices.size == dimension) {
            tokens += INC
            tokens += visit(indices.last())
            tokens += ADD
        }

        if (node.name.isGlobal) {
            tokens += GLOB
        }

        tokens += instruction

        return tokens
    }
}