package kakkoiichris.svml.lang

import kakkoiichris.svml.lang.lexer.Context
import kakkoiichris.svml.lang.lexer.TokenType
import kakkoiichris.svml.lang.parser.*
import kakkoiichris.svml.linker.Linker
import kakkoiichris.svml.util.svmlError

object Semantics : Node.Visitor<DataType> {
    private val memory = Memory()

    fun check(program: Node.Program) =
        visit(program)

    override fun visitProgram(node: Node.Program): DataType {
        node.declarations.forEach(::visit)

        node.functions.forEach(::prepareFunction)

        node.functions.forEach(::visit)

        if (node.functions.none { isMainFunction(it, node.context.source) }) {
            svmlError("No main function", node.context.source, node.context)
        }

        val mainReturn = implicitMainReturn()

        visit(mainReturn)

        node.mainReturn = mainReturn

        memory.clear()

        return DataType.Primitive.VOID
    }

    private fun prepareFunction(node: Node.Function) {
        memory.addFunction(node.type.value, node.signature, node.isNative)
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

        val (_, dataType, id) = memory.getFunction(mainSignature)

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

        memory.addVariable(node.isConstant, node.isMutable, node.name.value, type, node.context)

        val (isGlobal, record) = memory.getVariable(node.name)

        node.name.isGlobal = isGlobal

        val (_, _, _, id) = record

        node.type = Type(node.context, type)
        node.dataType = type
        node.id = id
        node.name.id = id

        return DataType.Primitive.VOID
    }

    override fun visitIf(node: Node.If): DataType {
        for (branch in node.branches) {
            branch.condition?.let { visit(it) }

            try {
                memory.push()

                for (subNode in branch.body) {
                    visit(subNode)
                }
            }
            finally {
                memory.pop()
            }
        }

        return DataType.Primitive.VOID
    }

    override fun visitWhile(node: Node.While): DataType {
        visit(node.condition)

        try {
            memory.push()

            for (subNode in node.body) {
                visit(subNode)
            }
        }
        finally {
            memory.pop()
        }

        return DataType.Primitive.VOID
    }

    override fun visitDo(node: Node.Do): DataType {
        try {
            memory.push()

            for (subNode in node.body) {
                visit(subNode)
            }
        }
        finally {
            memory.pop()
        }

        visit(node.condition)

        return DataType.Primitive.VOID
    }

    override fun visitFor(node: Node.For): DataType {
        node.init?.let { visit(it) }
        node.condition?.let { visit(it) }
        node.increment?.let { visit(it) }

        try {
            memory.push()

            for (subNode in node.body) {
                visit(subNode)
            }
        }
        finally {
            memory.pop()
        }

        return DataType.Primitive.VOID
    }

    override fun visitBreak(node: Node.Break): DataType {
        return DataType.Primitive.VOID
    }

    override fun visitContinue(node: Node.Continue): DataType {
        return DataType.Primitive.VOID
    }

    override fun visitFunction(node: Node.Function): DataType {
        try {
            memory.push()

            for (param in node.params) {
                visit(param)
            }

            for (subNode in node.body) {
                visit(subNode)
            }
        }
        finally {
            memory.pop()
        }

        if (node.isNative) {
            if (!Linker.hasFunction(node.id)) {
                svmlError("No function link available for '${node.signature}'", node.context)
            }

            return DataType.Primitive.VOID
        }

        if (DataType.isEquivalent(
                node.type.value,
                DataType.Primitive.VOID,
                node.context.source
            ) && node.body.lastOrNull() !is Node.Return
        ) {
            node.body += Node.Return(
                Context.none(),
                Node.Value(Context.none(), TokenType.Value(0.0, DataType.Primitive.VOID))
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

        resolveBranchReturnTypes(node.body, returnType)

        return DataType.Primitive.VOID
    }

    private fun resolveBranches(parentContext: Context, nodes: Nodes) {
        when (val last = nodes.lastOrNull()) {
            is Node.Return -> return

            is Node.If     -> {
                if (last.branches.last().condition != null) {
                    svmlError("Final if statement must return a value from else branch", last.context)
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
            checkUnreachable(getSubNodes(node))

            if (node !is Node.Return) continue

            if (i == nodes.lastIndex) continue

            svmlError("Unreachable code", node.context.source, nodes[i + 1].context)
        }
    }

    private fun getPrimaryReturn(nodes: Nodes): Node.Return = when (val last = nodes.last()) {
        is Node.If     -> getPrimaryReturn(last.branches.first().body)

        is Node.While  -> getPrimaryReturn(last.body)

        is Node.Do     -> getPrimaryReturn(last.body)

        is Node.For    -> getPrimaryReturn(last.body)

        is Node.Return -> last

        else           -> svmlError("Function does not have a primary return", last.context.source, last.context)
    }

    private fun resolveBranchReturnTypes(nodes: Nodes, dataType: DataType) {
        for (node in nodes) {
            if (node is Node.Return && node.dataType != dataType) {
                svmlError("All paths must return the same type", node.context.source, node.context)
            }

            resolveBranchReturnTypes(getSubNodes(node), dataType)//TODO RETURN TOO EARLY?
        }
    }

    private fun getSubNodes(node: Node): Nodes = when (node) {
        is Node.If    -> node.branches.flatMap { it.body }

        is Node.While -> node.body

        is Node.Do    -> node.body

        is Node.For   -> node.body

        else          -> emptyList()
    }.toMutableList()

    override fun visitReturn(node: Node.Return): DataType {
        val type = node.value?.let { visit(it) } ?: DataType.Primitive.VOID

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
        val (isGlobal, record) = memory.getVariable(node)

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
            svmlError("Array literals must be homogenous", node.context)
        }

        val type = DataType.Array(first, types.size)

        node.dataType = type

        return type
    }

    override fun visitUnary(node: Node.Unary): DataType {
        val type = when (val operator = node.operator) {
            Node.Unary.Operator.NEGATE -> when (val operand = visit(node.operand)) {
                DataType.Primitive.FLOAT -> DataType.Primitive.FLOAT
                DataType.Primitive.INT   -> DataType.Primitive.INT
                else                     -> invalidUnaryOperand(operator, operand, node.operand.context)
            }

            Node.Unary.Operator.INVERT -> when (val operand = visit(node.operand)) {
                DataType.Primitive.BOOL -> DataType.Primitive.BOOL
                else                    -> invalidUnaryOperand(operator, operand, node.operand.context)
            }
        }

        node.dataType = type

        return type
    }

    private fun invalidUnaryOperand(operator: Node.Unary.Operator, operand: DataType, context: Context): Nothing =
        svmlError("Unary '${operator.symbol}' operator not applicable for values of type '$operand'", context)

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
        val type = when (val operator = node.operator) {
            Node.Binary.Operator.EQUAL,
            Node.Binary.Operator.NOT_EQUAL     -> when (val left = visit(node.operandLeft)) {
                DataType.Primitive.BOOL  -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.BOOL -> DataType.Primitive.BOOL
                    else                    -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                DataType.Primitive.CHAR  -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.CHAR -> DataType.Primitive.BOOL
                    else                    -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                DataType.Primitive.FLOAT -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.BOOL
                    DataType.Primitive.INT   -> DataType.Primitive.BOOL
                    else                     -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                DataType.Primitive.INT   -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.BOOL
                    DataType.Primitive.INT   -> DataType.Primitive.BOOL
                    else                     -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                else                     -> invalidLeftOperand(operator, left, node.operandLeft.context)
            }

            Node.Binary.Operator.LESS,
            Node.Binary.Operator.LESS_EQUAL,
            Node.Binary.Operator.GREATER,
            Node.Binary.Operator.GREATER_EQUAL -> when (val left = visit(node.operandLeft)) {
                DataType.Primitive.CHAR  -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.CHAR -> DataType.Primitive.BOOL
                    else                    -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                DataType.Primitive.FLOAT -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.BOOL
                    DataType.Primitive.INT   -> DataType.Primitive.BOOL
                    else                     -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                DataType.Primitive.INT   -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.BOOL
                    DataType.Primitive.INT   -> DataType.Primitive.BOOL
                    else                     -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                else                     -> invalidLeftOperand(operator, left, node.operandLeft.context)
            }

            Node.Binary.Operator.ADD,
            Node.Binary.Operator.SUBTRACT      -> when (val left = visit(node.operandLeft)) {
                DataType.Primitive.CHAR  -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.INT -> DataType.Primitive.CHAR
                    else                   -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                DataType.Primitive.FLOAT -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.FLOAT
                    DataType.Primitive.INT   -> DataType.Primitive.FLOAT
                    else                     -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                DataType.Primitive.INT   -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.FLOAT
                    DataType.Primitive.INT   -> DataType.Primitive.INT
                    else                     -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                else                     -> invalidLeftOperand(operator, left, node.operandLeft.context)
            }

            Node.Binary.Operator.MULTIPLY,
            Node.Binary.Operator.DIVIDE,
            Node.Binary.Operator.INT_DIVIDE,
            Node.Binary.Operator.MODULUS,
            Node.Binary.Operator.INT_MODULUS   -> when (val left = visit(node.operandLeft)) {
                DataType.Primitive.FLOAT -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.FLOAT
                    DataType.Primitive.INT   -> DataType.Primitive.FLOAT
                    else                     -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                DataType.Primitive.INT   -> when (val right = visit(node.operandRight)) {
                    DataType.Primitive.FLOAT -> DataType.Primitive.FLOAT
                    DataType.Primitive.INT   -> DataType.Primitive.INT
                    else                     -> invalidRightOperand(operator, right, node.operandRight.context)
                }

                else                     -> invalidLeftOperand(operator, left, node.operandLeft.context)
            }
        }

        node.dataType = type

        return type
    }

    private fun invalidLeftOperand(operator: Node.Binary.Operator, operand: DataType, context: Context): Nothing =
        svmlError(
            "Binary '${operator.symbol}' operator not applicable for left hand values of type '$operand'",
            context
        )


    private fun invalidRightOperand(operator: Node.Binary.Operator, operand: DataType, context: Context): Nothing =
        svmlError(
            "Binary '${operator.symbol}' operator not applicable for right hand values of type '$operand'",
            context
        )


    override fun visitLogical(node: Node.Logical): DataType {
        val type = when (val left = visit(node.operandLeft)) {
            DataType.Primitive.BOOL -> when (val right = visit(node.operandRight)) {
                DataType.Primitive.BOOL -> DataType.Primitive.BOOL
                else                    -> svmlError(
                    "Logical operator not applicable for right hand values of type '$right'",
                    node.operandRight.context
                )
            }

            else                    -> svmlError(
                "Logical operator not applicable for left hand values of type '$left'",
                node.operandLeft.context
            )
        }

        node.dataType = type

        return type
    }

    override fun visitAssign(node: Node.Assign): DataType {
        val (_, record) = memory.getVariable(node.name)

        val (isConstant, isMutable, dataType, _) = record

        if (isConstant) {
            svmlError("Constant '${node.name.value}' cannot be reassigned", node.name.context)
        }

        if (isMutable && !DataType.isArray(dataType, node.context.source)) {
            svmlError("Non-array values cannot be marked as mutable", node.name.context)
        }

        val assigned = visit(node.assigned)

        if (!DataType.isEquivalent(dataType, assigned, node.context.source)) {
            svmlError(
                "Cannot assign a value of type '$assigned' to a variable of type '$dataType'",
                node.assigned.context
            )
        }

        return DataType.Primitive.VOID
    }

    override fun visitInvoke(node: Node.Invoke): DataType {
        val args = node.args.map { visit(it) }

        val signature = Signature(node.name, args)

        val (isNative, dataType, id) = memory.getFunction(signature)

        node.isNative = isNative
        node.id = id
        node.dataType = dataType

        return dataType
    }

    override fun visitGetIndex(node: Node.GetIndex): DataType {
        node.indices.forEach {
            if (visit(it) != DataType.Primitive.INT) {
                svmlError("Array index must be of type '${DataType.Primitive.INT}'", it.context)
            }
        }

        var type = DataType.asArray(visit(node.name), node.context.source)

        repeat(node.indices.size - 1) {
            if (!DataType.isArray(type, node.context.source)) {
                svmlError("Cannot index a value of type '$type'", node.context)
            }

            type = DataType.asArray(type.subType, node.context.source)
        }

        node.dataType = type

        return type
    }

    override fun visitSetIndex(node: Node.SetIndex): DataType {
        node.indices.forEach {
            if (visit(it) != DataType.Primitive.INT) {
                svmlError("Array index must be of type '${DataType.Primitive.INT}'", it.context)
            }
        }

        var type = DataType.asArray(visit(node.name), node.context.source)

        repeat(node.indices.size - 1) {
            if (!DataType.isArray(type, node.context.source)) {
                svmlError("Cannot index a value of type '$type'", node.name.context)
            }

            type = DataType.asArray(type.subType, node.context.source)
        }

        val assigned = visit(node.value)

        if (!DataType.isEquivalent(type.subType, assigned, node.context.source)) {
            svmlError(
                "Cannot assign a value of type '$assigned' to a variable of type '${type.subType}'",
                node.value.context
            )
        }

        return DataType.Primitive.VOID
    }
}