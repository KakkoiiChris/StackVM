package kakkoiichris.svml.lang

import kakkoiichris.svml.lang.lexer.Context
import kakkoiichris.svml.lang.lexer.TokenType
import kakkoiichris.svml.lang.parser.*
import kakkoiichris.svml.lang.parser.DataType.Primitive.VOID
import kakkoiichris.svml.util.svmlError

object Semantics : Node.Visitor<DataType> {
    fun check(program: Node.Program) =
        visit(program)

    override fun visitProgram(node: Node.Program): DataType {
        node.declarations.forEach(::visit)

        node.functions.forEach(::prepareFunction)

        node.functions.forEach(::visit)

        if (node.functions.none { isMainFunction(it, node.context.source) }) {
            svmlError("No main function", node.context.source, node.context)
        }

        node.mainReturn = implicitMainReturn()

        Memory.clear()

        return VOID
    }

    private fun prepareFunction(node: Node.Function) {
        val signature = Signature(node.name, node.params.map { it.type!!.value })

        Memory.addFunction(node.type.value, signature, node.isNative)
    }

    private fun isMainFunction(stmt: Node, source: Source) =
        stmt is Node.Function &&
            stmt.name.value == "main" && (
            DataType.isEquivalent(stmt.dataType, DataType.Primitive.INT, source) ||
                DataType.isEquivalent(stmt.dataType, DataType.Primitive.VOID, source)
            )

    private fun implicitMainReturn(): Node.Return {
        val context = Context.none()

        val name = Node.Name(context, "main")

        val mainSignature = Signature(name, emptyList())

        val (_, dataType, id) = Memory.getFunction(mainSignature)

        val invokeMain = Node.Invoke(context, name, mutableListOf())

        invokeMain.dataType = dataType
        invokeMain.id = id

        return Node.Return(context, invokeMain)
    }

    override fun visitDeclare(node: Node.Declare): DataType {
        val assignedType = node.assigned?.let { visit(it) }

        val type = node.type?.value
            ?: assignedType
            ?: svmlError("Variable must at least be typed or assigned", node.context.source, node.context)

        if (assignedType != null && !DataType.isEquivalent(type, assignedType, node.context.source)) {
            svmlError(
                "Cannot declare a variable of type '${type}' with value of type '${assignedType}'",
                node.context.source,
                node.context
            )
        }

        Memory.addVariable(node.isConstant, node.isMutable, node.name.value, type, node.context)

        val (isGlobal, record) = Memory.getVariable(node.name)

        node.name.isGlobal = isGlobal

        val (_, _, _, id) = record

        node.type = Type(node.context, type)
        node.dataType = type
        node.id = id
        node.name.id = id

        return VOID
    }

    override fun visitIf(node: Node.If): DataType {
        for (branch in node.branches) {
            branch.condition?.let { visit(it) }

            try {
                Memory.push()

                for (subNode in branch.body) {
                    visit(subNode)
                }
            }
            finally {
                Memory.pop()
            }
        }

        return VOID
    }

    override fun visitWhile(node: Node.While): DataType {
        visit(node.condition)

        try {
            Memory.push()

            for (subNode in node.body) {
                visit(subNode)
            }
        }
        finally {
            Memory.pop()
        }

        return VOID
    }

    override fun visitDo(node: Node.Do): DataType {
        try {
            Memory.push()

            for (subNode in node.body) {
                visit(subNode)
            }
        }
        finally {
            Memory.pop()
        }

        visit(node.condition)

        return VOID
    }

    override fun visitFor(node: Node.For): DataType {
        node.init?.let { visit(it) }
        node.condition?.let { visit(it) }
        node.increment?.let { visit(it) }

        try {
            Memory.push()

            for (subNode in node.body) {
                visit(subNode)
            }
        }
        finally {
            Memory.pop()
        }

        return VOID
    }

    override fun visitBreak(node: Node.Break): DataType {
        return VOID
    }

    override fun visitContinue(node: Node.Continue): DataType {
        return VOID
    }

    override fun visitFunction(node: Node.Function): DataType {
        try {
            Memory.push()

            for (param in node.params) {
                visit(param)
            }

            for (subNode in node.body) {
                visit(subNode)
            }
        }
        finally {
            Memory.pop()
        }

        if (!node.isNative) {
            if (DataType.isEquivalent(node.type.value, VOID, node.context.source) && node.body.last() !is Node.Return) {
                node.body += Node.Return(
                    Context.none(),
                    Node.Value(Context.none(), TokenType.Value(0.0, VOID))
                )
            }

            resolveBranches(node.context, node.body)

            checkUnreachable(node.body)

            val primaryReturn = getPrimaryReturn(node.body)

            val returnType = primaryReturn.dataType

            if (!DataType.isEquivalent(returnType, node.type.value, node.context.source)) {
                svmlError(
                    "Function must return value of type '${node.type.value}'",
                    node.context.source,
                    primaryReturn.context
                )
            }

            resolveBranchReturns(returnType, node.body)
        }

        return VOID
    }

    private fun resolveBranches(parentContext: Context, nodes: Nodes) {
        when (val last = nodes.lastOrNull()) {
            is Node.Return -> return

            is Node.If     -> {
                if (last.branches.last().condition != null) {
                    svmlError(
                        "Final if statement must return a value from else branch",
                        last.context.source,
                        last.context
                    )
                }

                for (branch in last.branches) {
                    resolveBranches(branch.context, branch.body)
                }
            }

            is Node.While  -> resolveBranches(last.context, last.body)

            is Node.Do     -> resolveBranches(last.context, last.body)

            is Node.For    -> resolveBranches(last.context, last.body)

            null           -> svmlError("Function does not return a value", parentContext.source, parentContext)

            else           -> svmlError("Function does not return a value", last.context.source, last.context)
        }
    }

    private fun checkUnreachable(nodes: Nodes) {
        for ((i, node) in nodes.withIndex()) {
            if (!isOrHasReturns(node)) continue

            if (i == nodes.lastIndex) continue

            if (node !is Node.If) continue

            if (!node.branches.all { branch -> branch.body.lastOrNull()?.let { isOrHasReturns(it) } == true }) continue

            if (node.branches.last().condition != null) continue

            svmlError("Unreachable code", node.context.source, nodes[i + 1].context)
        }
    }

    private fun isOrHasReturns(node: Node): Boolean = when (node) {
        is Node.If     -> node.branches.any { branch -> branch.body.any(::isOrHasReturns) }

        is Node.While  -> node.body.any(::isOrHasReturns)

        is Node.Do     -> node.body.any(::isOrHasReturns)

        is Node.For    -> node.body.any(::isOrHasReturns)

        is Node.Return -> true

        else           -> false
    }

    private fun getPrimaryReturn(nodes: Nodes): Node.Return = when (val last = nodes.last()) {
        is Node.If     -> getPrimaryReturn(last.branches.first().body)

        is Node.While  -> getPrimaryReturn(last.body)

        is Node.Do     -> getPrimaryReturn(last.body)

        is Node.For    -> getPrimaryReturn(last.body)

        is Node.Return -> last

        else           -> svmlError("Function does not have a primary return", last.context.source, last.context)
    }

    private fun resolveBranchReturns(dataType: DataType, nodes: Nodes) {
        for (node in nodes) {
            if (node is Node.Return && node.dataType != dataType) {
                svmlError("All paths must return the same type", node.context.source, node.context)
            }

            return resolveBranchReturns(dataType, getSubNodes(node))//TODO RETURN TOO EARLY?
        }
    }

    private fun getSubNodes(node: Node): Nodes = when (node) {
        is Node.If    -> node.branches.flatMap { it.body }

        is Node.While -> node.body

        is Node.Do    -> node.body

        is Node.For   -> node.body

        else          -> emptyList()
    }.toMutableList()

    override

    fun visitReturn(node: Node.Return): DataType {
        val type = node.value?.let { visit(it) } ?: VOID

        node.dataType = type

        return type
    }

    override fun visitExpression(node: Node.Expression): DataType {
        return visit(node.node)
    }

    override fun visitValue(node: Node.Value): DataType {
        val type = node.value.dataType

        node.dataType = type

        return type
    }

    override fun visitString(node: Node.String): DataType {
        val type = DataType.string

        node.dataType = type

        return type
    }

    override fun visitName(node: Node.Name): DataType {
        val (isGlobal, record) = Memory.getVariable(node)

        node.isGlobal = isGlobal

        val (_, _, dataType, id) = record

        node.dataType = dataType
        node.id = id

        return dataType
    }

    override fun visitArray(node: Node.Array): DataType {
        val types = node.elements.map { visit(it) }

        val first = types.first()

        if (!types.all { it == first }) {
            TODO()
        }

        val type = DataType.Array(first, types.size)

        node.dataType = type

        return type
    }

    override fun visitUnary(node: Node.Unary): DataType {
        val type = when (node.operator) {
            Node.Unary.Operator.NEGATE -> when (visit(node.operand)) {
                DataType.Primitive.FLOAT -> DataType.Primitive.FLOAT
                DataType.Primitive.INT   -> DataType.Primitive.INT
                else                     -> TODO()
            }

            Node.Unary.Operator.INVERT -> when (visit(node.operand)) {
                DataType.Primitive.BOOL -> DataType.Primitive.BOOL
                else                    -> TODO()
            }
        }

        node.dataType = type

        return type
    }

    override fun visitSize(node: Node.Size): DataType {
        visit(node.name)

        val type = DataType.Primitive.INT

        node.dataType = type

        return type
    }

    override fun visitIndexSize(node: Node.IndexSize): DataType {
        val type = DataType.Primitive.INT

        node.dataType = type

        return type
    }

    override fun visitBinary(node: Node.Binary): DataType {
        val type = when (node.operator) {
            Node.Binary.Operator.EQUAL,
            Node.Binary.Operator.NOT_EQUAL     -> when (visit(node.operandLeft)) {
                DataType.Primitive.BOOL  -> when (visit(node.operandRight)) {
                    DataType.Primitive.BOOL -> DataType.Primitive.BOOL
                    else                    -> TODO()
                }

                DataType.Primitive.CHAR  -> when (visit(node.operandRight)) {
                    DataType.Primitive.CHAR -> DataType.Primitive.BOOL
                    else                    -> TODO()
                }

                DataType.Primitive.FLOAT -> when (visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.BOOL
                    DataType.Primitive.INT   -> DataType.Primitive.BOOL
                    else                     -> TODO()
                }

                DataType.Primitive.INT   -> when (visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.BOOL
                    DataType.Primitive.INT   -> DataType.Primitive.BOOL
                    else                     -> TODO()
                }

                else                     -> TODO()
            }

            Node.Binary.Operator.LESS,
            Node.Binary.Operator.LESS_EQUAL,
            Node.Binary.Operator.GREATER,
            Node.Binary.Operator.GREATER_EQUAL -> when (visit(node.operandLeft)) {
                DataType.Primitive.CHAR  -> when (visit(node.operandRight)) {
                    DataType.Primitive.CHAR -> DataType.Primitive.BOOL
                    else                    -> TODO()
                }

                DataType.Primitive.FLOAT -> when (visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.BOOL
                    DataType.Primitive.INT   -> DataType.Primitive.BOOL
                    else                     -> TODO()
                }

                DataType.Primitive.INT   -> when (visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.BOOL
                    DataType.Primitive.INT   -> DataType.Primitive.BOOL
                    else                     -> TODO()
                }

                else                     -> TODO()
            }

            Node.Binary.Operator.ADD,
            Node.Binary.Operator.SUBTRACT      -> when (visit(node.operandLeft)) {
                DataType.Primitive.CHAR  -> when (visit(node.operandRight)) {
                    DataType.Primitive.INT -> DataType.Primitive.CHAR
                    else                   -> TODO()
                }

                DataType.Primitive.FLOAT -> when (visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.FLOAT
                    DataType.Primitive.INT   -> DataType.Primitive.FLOAT
                    else                     -> TODO()
                }

                DataType.Primitive.INT   -> when (visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.FLOAT
                    DataType.Primitive.INT   -> DataType.Primitive.INT
                    else                     -> TODO()
                }

                else                     -> TODO()
            }

            Node.Binary.Operator.MULTIPLY,
            Node.Binary.Operator.DIVIDE,
            Node.Binary.Operator.INT_DIVIDE,
            Node.Binary.Operator.MODULUS,
            Node.Binary.Operator.INT_MODULUS   -> when (visit(node.operandLeft)) {
                DataType.Primitive.FLOAT -> when (visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.FLOAT
                    DataType.Primitive.INT   -> DataType.Primitive.FLOAT
                    else                     -> TODO()
                }

                DataType.Primitive.INT   -> when (visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.FLOAT
                    DataType.Primitive.INT   -> DataType.Primitive.INT
                    else                     -> TODO()
                }

                else                     -> TODO()
            }
        }

        node.dataType = type

        return type
    }

    override fun visitLogical(node: Node.Logical): DataType {
        val type = when (visit(node.operandLeft)) {
            DataType.Primitive.BOOL -> when (visit(node.operandRight)) {
                DataType.Primitive.BOOL -> DataType.Primitive.BOOL
                else                    -> TODO("RIGHT NOT BOOL")
            }

            else                    -> TODO("LEFT NOT BOOL")
        }

        node.dataType = type

        return type
    }

    override fun visitAssign(node: Node.Assign): DataType {
        val (_, record) = Memory.getVariable(node.name)

        val (isConstant, isMutable, dataType, _) = record

        if (isConstant) {
            svmlError("Constant '${node.name.value}' cannot be reassigned", node.context.source, node.name.context)
        }

        if (isMutable && !DataType.isArray(dataType, node.context.source)) {
            svmlError("Non-array values cannot be marked as mutable", node.context.source, node.name.context)
        }

        val assigned = visit(node.assigned)

        if (!DataType.isEquivalent(dataType, assigned, node.context.source)) {
            TODO("TYPE MISMATCH")
        }

        return VOID
    }

    override fun visitInvoke(node: Node.Invoke): DataType {
        val signature = Signature(node.name, node.args.map { visit(it) })

        val (isNative, dataType, id) = Memory.getFunction(signature)

        node.isNative = isNative
        node.id = id
        node.dataType = dataType

        return dataType
    }

    override fun visitGetIndex(node: Node.GetIndex): DataType {
        node.indices.forEach { visit(it) }

        var superType = DataType.asArray(visit(node.name), node.context.source)

        repeat(node.indices.size - 1) {
            if (!DataType.isArray(superType.subType, node.context.source)) {
                TODO()
            }

            superType = DataType.asArray(superType.subType, node.context.source)
        }

        val type = superType.subType

        node.dataType = type

        return type
    }

    override fun visitSetIndex(node: Node.SetIndex): DataType {
        node.indices.forEach { visit(it) }

        var superType = DataType.asArray(visit(node.name), node.context.source)

        repeat(node.indices.size) {
            if (!DataType.isArray(superType.subType, node.context.source)) {
                TODO()
            }

            superType = DataType.asArray(superType.subType, node.context.source)
        }

        val assigned = visit(node.value)

        if (!DataType.isEquivalent(superType.subType, assigned, node.context.source)) {
            TODO()
        }

        return VOID
    }
}