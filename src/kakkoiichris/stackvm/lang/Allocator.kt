package kakkoiichris.stackvm.lang

import kakkoiichris.stackvm.lang.parser.Node
import kakkoiichris.stackvm.lang.parser.Nodes
import java.util.*

object Allocator : Node.Visitor<Unit> {
    private val offsets = Stack<Int>()

    private val addresses = mutableMapOf<Int, Int>()

    fun allocate(program: Node.Program) {
        visit(program)
    }

    private fun allocateDecls(nodes: Nodes, startAddress: Int): Int {
        var addressCounter = startAddress

        val singles = nodes.filterIsInstance<Node.DeclareSingle>()

        for (decl in singles) {
            val address = addressCounter++

            addresses[decl.id] = address

            decl.address = address
        }

        val arrays = nodes.filterIsInstance<Node.DeclareArray>()

        for (decl in arrays) {
            if (decl.variable.dataType.isHeapAllocated(decl.context.source)) continue

            val address = addressCounter

            addressCounter += decl.variable.dataType.getOffset(decl.variable.context.source)

            addresses[decl.id] = address

            decl.address = address
        }

        return addressCounter
    }

    private fun allocateSingle(node: Node.DeclareSingle, startAddress: Int): Int {
        node.address = startAddress

        addresses[node.id] = startAddress

        return startAddress + 1
    }

    private fun allocateArray(node: Node.DeclareArray, startAddress: Int): Int {
        node.address = startAddress

        addresses[node.id] = startAddress

        return startAddress + node.getDataType(node.context.source)!!.getOffset(node.context.source)
    }

    override fun visitProgram(node: Node.Program) {
        val offset = allocateDecls(node.statements, 0)

        node.offset = offset

        for (statement in node.statements) {
            offsets.push(offset)

            visit(statement)
        }
    }

    override fun visitDeclareSingle(node: Node.DeclareSingle) {
        visit(node.variable)

        node.node?.let { visit(it) }
    }

    override fun visitDeclareArray(node: Node.DeclareArray) {
        visit(node.variable)

        node.node?.let { visit(it) }
    }

    override fun visitIf(node: Node.If) {
        val startAddress = offsets.pop()

        for ((_, condition, body) in node.branches) {
            val offset = allocateDecls(body, startAddress)

            condition?.let { visit(it) }

            for (statement in body) {
                offsets.push(offset)

                visit(statement)
            }
        }
    }

    override fun visitWhile(node: Node.While) {
        var offset = offsets.pop()

        offset = allocateDecls(node.body, offset)

        visit(node.condition)

        for (statement in node.body) {
            offsets.push(offset)

            visit(statement)
        }
    }

    override fun visitDo(node: Node.Do) {
        var offset = offsets.pop()

        offset = allocateDecls(node.body, offset)

        visit(node.condition)

        for (statement in node.body) {
            offsets.push(offset)

            visit(statement)
        }
    }

    override fun visitFor(node: Node.For) {
        var offset = offsets.pop()

        if (node.init != null) {
            offset = allocateSingle(node.init, offset)
        }

        node.init?.let { visit(it) }

        node.condition?.let { visit(it) }

        node.increment?.let { visit(it) }

        offset = allocateDecls(node.body, offset)

        for (statement in node.body) {
            offsets.push(offset)

            visit(statement)
        }
    }

    override fun visitBreak(node: Node.Break) = Unit

    override fun visitContinue(node: Node.Continue) = Unit

    override fun visitFunction(node: Node.Function) {
        var offset = 0

        for (param in node.params) {
            if (param.dataType.isHeapAllocated(param.context.source)) continue

            param.address = offset

            addresses[param.id] = offset

            offset += param.dataType.getOffset(param.context.source)
        }

        offset = allocateDecls(node.body, offset)

        for (statement in node.body) {
            offsets.push(offset)

            visit(statement)
        }

        node.offset = offset
    }

    override fun visitReturn(node: Node.Return) {
        node.node?.let { visit(it) }
    }

    override fun visitExpression(node: Node.Expression) {
        visit(node.node)
    }

    override fun visitValue(node: Node.Value) = Unit

    override fun visitString(node: Node.String) = Unit

    override fun visitVariable(node: Node.Variable) {
        if (node.dataType.isHeapAllocated(node.context.source)) return

        node.address = addresses[node.id]!!
    }

    override fun visitArray(node: Node.Array) {
        for (element in node.elements) {
            visit(element)
        }
    }

    override fun visitUnary(node: Node.Unary) {
        visit(node.operand)
    }

    override fun visitSize(node: Node.Size) {
        visit(node.variable)
    }

    override fun visitIndexSize(node: Node.IndexSize) {
        visit(node.variable)

        for (index in node.indices) {
            visit(index)
        }
    }

    override fun visitBinary(node: Node.Binary) {
        visit(node.operandLeft)

        visit(node.operandRight)
    }

    override fun visitLogical(node: Node.Logical) {
        visit(node.operandLeft)

        visit(node.operandRight)
    }

    override fun visitAssign(node: Node.Assign) {
        visit(node.variable)

        visit(node.node)
    }

    override fun visitInvoke(node: Node.Invoke) {
        node.offset = offsets.peek()

        for (arg in node.args) {
            visit(arg)
        }
    }

    override fun visitSystemCall(node: Node.SystemCall) {
        for (arg in node.args) {
            visit(arg)
        }
    }

    override fun visitGetIndex(node: Node.GetIndex) {
        visit(node.variable)

        for (index in node.indices) {
            visit(index)
        }
    }

    override fun visitSetIndex(node: Node.SetIndex) {
        visit(node.variable)

        for (index in node.indices) {
            visit(index)
        }

        visit(node.value)
    }
}