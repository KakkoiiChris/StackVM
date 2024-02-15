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
            val address = addressCounter

            addressCounter += decl.variable.dataType.offset

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

        return startAddress + node.dataType!!.offset
    }

    override fun visitProgram(node: Node.Program) {
        val offset = allocateDecls(node.statements, 0)

        for (statement in node.statements) {
            offsets.push(offset)

            visit(statement)
        }
    }

    override fun visitDeclareSingle(node: Node.DeclareSingle) {
        visit(node.variable)

        if (node.node != null) {
            visit(node.node)
        }
    }

    override fun visitDeclareArray(node: Node.DeclareArray) {
        visit(node.variable)

        if (node.node != null) {
            visit(node.node)
        }
    }

    override fun visitIf(node: Node.If) {
        val startAddress = offsets.pop()

        for ((_, condition, body) in node.branches) {
            val offset = allocateDecls(body, startAddress)

            if (condition != null) {
                visit(condition)
            }

            for (statement in body) {
                offsets.push(offset)

                visit(statement)
            }
        }
    }

    override fun visitWhile(node: Node.While) {
        val startAddress = offsets.pop()

        val offset = allocateDecls(node.body, startAddress)

        visit(node.condition)

        for (statement in node.body) {
            offsets.push(offset)

            visit(statement)
        }
    }

    override fun visitDo(node: Node.Do) {
        val startAddress = offsets.pop()

        val offset = allocateDecls(node.body, startAddress)

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
        val initialOffset = offsets.pop()

        node.offset = initialOffset

        var addressCounter = 0

        for (param in node.params) {
            param.address = addressCounter

            addresses[param.id] = addressCounter

            addressCounter += param.dataType.offset
        }

        allocateDecls(node.body, addressCounter)

        for (statement in node.body) {
            offsets.push(addressCounter)

            visit(statement)
        }
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
        node.address = addresses[node.id]!!
    }

    override fun visitType(node: Node.Type) = Unit

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

    override fun visitBinary(node: Node.Binary) {
        visit(node.operandLeft)

        visit(node.operandRight)
    }

    override fun visitAssign(node: Node.Assign) {
        visit(node.variable)

        visit(node.node)
    }

    override fun visitInvoke(node: Node.Invoke) {
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

    override fun visitName(node: Node.Name) = Unit
}