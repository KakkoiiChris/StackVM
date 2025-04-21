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
package kakkoiichris.svml.lang

import kakkoiichris.svml.lang.parser.DataType
import kakkoiichris.svml.lang.parser.Node
import kakkoiichris.svml.lang.parser.Nodes
import kakkoiichris.svml.util.svmlError
import java.util.*

object Allocator : Node.Visitor<Unit> {
    private val offsets = Stack<Int>()

    private val addresses = mutableMapOf<Int, Int>()

    fun allocate(program: Node.Program) {
        visit(program)
    }

    private fun allocateDecls(nodes: Nodes, startAddress: Int): Int {
        var addressCounter = startAddress

        val decls = nodes
            .filterIsInstance<Node.Declare>()
            .groupBy { DataType.isArray(it.dataType, it.context.source) }

        val singles = decls[false] ?: emptyList()

        for (decl in singles) {
            val address = addressCounter++

            addresses[decl.id] = address

            decl.address = address
            decl.name.address = address
        }

        val arrays = decls[true] ?: emptyList()

        for (decl in arrays) {
            if (decl.name.dataType.isHeapAllocated(decl.context.source)) continue

            val address = addressCounter

            addressCounter += decl.name.dataType.getOffset(decl.name.context.source)

            addresses[decl.id] = address

            decl.address = address
            decl.name.address = address
        }

        return addressCounter
    }

    private fun allocate(node: Node.Declare, startAddress: Int): Int {
        node.address = startAddress

        addresses[node.id] = startAddress

        return startAddress + node.dataType.getOffset(node.context.source)
    }

    override fun visitProgram(node: Node.Program) {
        val subNodes = (node.declarations + node.functions).toMutableList()

        val offset = allocateDecls(subNodes, 0)

        node.offset = offset

        for (statement in subNodes) {
            offsets.push(offset)

            visit(statement)
        }
    }

    override fun visitDeclare(node: Node.Declare) {
        visit(node.name)

        node.assigned?.let { visit(it) }
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
            offset = allocate(node.init, offset)
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
        node.value?.let { visit(it) }
    }

    override fun visitExpression(node: Node.Expression) {
        visit(node.node)
    }

    override fun visitValue(node: Node.Value) = Unit

    override fun visitString(node: Node.String) = Unit

    override fun visitName(node: Node.Name) {
        if (node.dataType.isHeapAllocated(node.context.source)) return

        node.address = addresses[node.id]
            ?: svmlError("Name '${node.value}' has no id", node.context.source, node.context)
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
        visit(node.name)
    }

    override fun visitIndexSize(node: Node.IndexSize) {
        visit(node.name)

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
        visit(node.name)

        visit(node.assigned)
    }

    override fun visitInvoke(node: Node.Invoke) {
        node.offset = offsets.peek()

        for (arg in node.args) {
            visit(arg)
        }
    }

    override fun visitGetIndex(node: Node.GetIndex) {
        visit(node.name)

        for (index in node.indices) {
            visit(index)
        }
    }

    override fun visitSetIndex(node: Node.SetIndex) {
        visit(node.name)

        for (index in node.indices) {
            visit(index)
        }

        visit(node.value)
    }
}