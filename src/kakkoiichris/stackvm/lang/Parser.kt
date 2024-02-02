package kakkoiichris.stackvm.lang

import kakkoiichris.stackvm.cpu.SystemFunctions

class Parser(private val lexer: Lexer, private val optimize: Boolean) : Iterator<Node> {
    private val memory = Memory()

    private var token = lexer.next()

    override fun hasNext() =
        !match(TokenType.End)

    override fun next() =
        statement()

    fun open() {
        SystemFunctions

        memory.open()
    }

    fun close() = memory.close()

    private fun here() = token.location

    private fun matchAny(vararg types: TokenType) =
        types.any { it == token.type }

    private fun match(type: TokenType) =
        type == token.type

    private inline fun <reified T : TokenType> match() =
        T::class.isInstance(token.type)

    private fun step() {
        if (lexer.hasNext()) {
            token = lexer.next()
        }
    }

    private fun skip(type: TokenType) =
        if (match(type)) {
            step()

            true
        }
        else false

    private fun mustSkip(type: TokenType) {
        if (!skip(type)) {
            error("Expected a '$type', but encountered a '${token.type}' @ ${here()}!")
        }
    }

    private inline fun <reified T : TokenType> get(): T? {
        if (match<T>()) {
            val token = token

            step()

            return token.type as T
        }

        return null
    }

    private fun statement() = when {
        matchAny(TokenType.Keyword.LET, TokenType.Keyword.VAR) -> declare()

        match(TokenType.Keyword.IF)                            -> `if`()

        match(TokenType.Keyword.WHILE)                         -> `while`()

        match(TokenType.Keyword.DO)                            -> `do`()

        match(TokenType.Keyword.FOR)                           -> `for`()

        match(TokenType.Keyword.BREAK)                         -> `break`()

        match(TokenType.Keyword.CONTINUE)                      -> `continue`()

        match(TokenType.Keyword.FUNCTION)                      -> function()

        match(TokenType.Keyword.RETURN)                        -> `return`()

        else                                                   -> expression()
    }

    private fun type(): Node.Type {
        val location = here()

        val baseType = when {
            skip(TokenType.Keyword.VOID)  -> DataType.Primitive.VOID

            skip(TokenType.Keyword.BOOL)  -> DataType.Primitive.BOOL

            skip(TokenType.Keyword.INT)   -> DataType.Primitive.INT

            skip(TokenType.Keyword.FLOAT) -> DataType.Primitive.FLOAT

            skip(TokenType.Keyword.CHAR)  -> DataType.Primitive.CHAR

            else                          -> error("Invalid type!")
        }

        var type: DataType = baseType

        if (match(TokenType.Symbol.LEFT_SQUARE)) {
            do {
                mustSkip(TokenType.Symbol.LEFT_SQUARE)

                val sizeNode = value()

                if (sizeNode.dataType != DataType.Primitive.INT) error("Array size must be an int @ ${sizeNode.location}!")

                val size = sizeNode.value.value.toInt()

                mustSkip(TokenType.Symbol.RIGHT_SQUARE)

                type = DataType.Array(type, size)
            }
            while (match(TokenType.Symbol.LEFT_SQUARE))
        }

        return Node.Type(location, TokenType.Type(type))
    }

    private fun declare(): Node {
        val location = here()

        val constant = skip(TokenType.Keyword.LET)

        if (!constant) {
            mustSkip(TokenType.Keyword.VAR)
        }

        val name = name()

        var type = if (skip(TokenType.Symbol.COLON)) type() else null

        mustSkip(TokenType.Symbol.EQUAL)

        val node = expr()

        if (type == null) {
            type = Node.Type(Location.none, TokenType.Type(node.dataType))
        }

        if (type.dataType != node.dataType) error("Cannot declare a variable of type '${type.dataType}' with value of type '${node.dataType}' @ ${node.location}!")

        val variable = createVariable(constant, name, type.dataType)

        val (_, activation) = memory
            .getVariable(variable)

        val (_, _, address) = activation

        mustSkip(TokenType.Symbol.SEMICOLON)

        if (type.type.value is DataType.Array) {
            return Node.DeclareArray(location, variable, address, node)
        }

        return Node.DeclareSingle(location, variable, address, node)
    }

    private fun `if`(): Node.If {
        val location = here()

        val branches = mutableListOf<Node.If.Branch>()

        while (true) {
            val branchLocation = here()

            val condition = if (skip(TokenType.Keyword.IF))
                expr()
            else
                null

            val body = mutableListOf<Node>()

            mustSkip(TokenType.Symbol.LEFT_BRACE)

            try {
                memory.push()

                while (!match(TokenType.Symbol.RIGHT_BRACE)) {
                    body += statement()
                }
            }
            finally {
                memory.pop()
            }

            mustSkip(TokenType.Symbol.RIGHT_BRACE)

            branches += Node.If.Branch(branchLocation, condition, body)

            if (!skip(TokenType.Keyword.ELSE)) break
        }

        return Node.If(location, branches)
    }

    private fun `while`(): Node.While {
        val location = here()

        mustSkip(TokenType.Keyword.WHILE)

        val condition = expr()

        val label = label()

        val body = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_BRACE)

        try {
            memory.push()

            while (!match(TokenType.Symbol.RIGHT_BRACE)) {
                body += statement()
            }
        }
        finally {
            memory.pop()
        }

        mustSkip(TokenType.Symbol.RIGHT_BRACE)

        return Node.While(location, condition, label, body)
    }

    private fun `do`(): Node.Do {
        val location = here()

        mustSkip(TokenType.Keyword.DO)

        val label = label()

        val body = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_BRACE)

        try {
            memory.push()

            while (!match(TokenType.Symbol.RIGHT_BRACE)) {
                body += statement()
            }
        }
        finally {
            memory.pop()
        }

        mustSkip(TokenType.Symbol.RIGHT_BRACE)
        mustSkip(TokenType.Keyword.WHILE)

        val condition = expr()

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Do(location, label, body, condition)
    }

    private fun `for`(): Node.For {
        val location = here()

        mustSkip(TokenType.Keyword.FOR)

        try {
            memory.push()

            var init: Node.DeclareSingle? = null

            if (!skip(TokenType.Symbol.SEMICOLON)) {
                val name = name()

                var type = if (skip(TokenType.Symbol.COLON)) type() else null

                mustSkip(TokenType.Symbol.EQUAL)

                val node = expr()

                if (type == null) {
                    type = Node.Type(Location.none, TokenType.Type(node.dataType))
                }

                val variable = createVariable(false, name, type.dataType)

                val (_, activation) = memory
                    .getVariable(variable)

                val (_, _, address) = activation

                init = Node.DeclareSingle(name.location, variable, address, node)

                mustSkip(TokenType.Symbol.SEMICOLON)
            }

            var condition: Node? = null

            if (!skip(TokenType.Symbol.SEMICOLON)) {
                condition = expr()

                mustSkip(TokenType.Symbol.SEMICOLON)
            }

            var increment: Node? = null

            if (!matchAny(TokenType.Symbol.AT, TokenType.Symbol.LEFT_BRACE)) {
                increment = expr()
            }

            val label = label()

            val body = mutableListOf<Node>()

            mustSkip(TokenType.Symbol.LEFT_BRACE)

            while (!match(TokenType.Symbol.RIGHT_BRACE)) {
                body += statement()
            }

            mustSkip(TokenType.Symbol.RIGHT_BRACE)

            return Node.For(location, init, condition, increment, label, body)
        }
        finally {
            memory.pop()
        }
    }

    private fun `break`(): Node.Break {
        val location = here()

        mustSkip(TokenType.Keyword.BREAK)

        val label = label()

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Break(location, label)
    }

    private fun `continue`(): Node.Continue {
        val location = here()

        mustSkip(TokenType.Keyword.CONTINUE)

        val label = label()

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Continue(location, label)
    }

    private fun function(): Node.Function {
        val location = here()

        mustSkip(TokenType.Keyword.FUNCTION)

        val name = name()

        val params = mutableListOf<Node.Variable>()

        var type = Node.Type(Location.none, TokenType.Type(DataType.Primitive.VOID))

        val body = mutableListOf<Node>()

        val id: Int
        val offset: Int

        var isNative = false

        try {
            id = memory.getFunctionID()

            memory.push()

            offset = memory.peek().variableID

            if (skip(TokenType.Symbol.LEFT_PAREN) && !skip(TokenType.Symbol.RIGHT_PAREN)) {
                do {
                    val paramName = name()

                    mustSkip(TokenType.Symbol.COLON)

                    val paramType = type()

                    params += createVariable(true, paramName, paramType.dataType)
                }
                while (skip(TokenType.Symbol.COMMA))

                mustSkip(TokenType.Symbol.RIGHT_PAREN)
            }

            if (skip(TokenType.Symbol.COLON)) type = type()

            if (skip(TokenType.Symbol.SEMICOLON)) {
                isNative = true
            }

            val here = memory.pop()!!
            memory.addFunction(type.dataType, id, Signature(name, params.map { it.dataType }), isNative)
            memory.push(here)

            if (!isNative) {
                if (skip(TokenType.Symbol.EQUAL)) {
                    body += expr()

                    mustSkip(TokenType.Symbol.SEMICOLON)
                }
                else if (skip(TokenType.Symbol.LEFT_BRACE)) {
                    while (!match(TokenType.Symbol.RIGHT_BRACE)) {
                        body += statement()
                    }

                    mustSkip(TokenType.Symbol.RIGHT_BRACE)
                }
            }
        }
        finally {
            memory.pop()
        }

        if (!isNative) {
            if (type.type.value == DataType.Primitive.VOID && body.last() !is Node.Return) {
                body += Node.Return(
                    Location.none,
                    Node.Value(Location.none, TokenType.Value(0F, DataType.Primitive.VOID))
                )
            }

            resolveBranches(body)

            checkUnreachable(body)

            val primaryReturn = getPrimaryReturn(body)

            val returnType = primaryReturn.dataType

            if (returnType != type.type.value) error("Function must return value of type '${type.type.value}' @ ${primaryReturn.location}!")

            resolveBranchReturns(returnType, body)
        }

        val function = Node.Function(location, name, id, offset, params, type, body)

        return function
    }

    private fun resolveBranches(nodes: Nodes) {
        val last = nodes.lastOrNull() ?: error("No returns!")

        when (last) {
            is Node.Return -> return

            is Node.If     -> {
                for (branch in last.branches) {
                    resolveBranches(branch.body)
                }
            }

            is Node.While  -> resolveBranches(last.body)

            is Node.Do     -> resolveBranches(last.body)

            is Node.For    -> resolveBranches(last.body)

            else           -> error("Not all branches!")
        }
    }

    private fun checkUnreachable(nodes: Nodes) {
        for ((i, node) in nodes.withIndex()) {
            if (!node.isOrHasReturns) continue

            if (i == nodes.lastIndex) continue

            if (node !is Node.If) continue

            if (!node.branches.all { it.body.lastOrNull()?.isOrHasReturns == true }) continue

            if (node.branches.last().condition != null) continue

            error("Unreachable code @ ${nodes[i + 1].location}!")
        }
    }

    private fun getPrimaryReturn(nodes: Nodes): Node.Return {
        val last = nodes.lastOrNull() ?: error("No last return!")

        return when (last) {
            is Node.Return -> last

            is Node.If     -> getPrimaryReturn(last.branches.first().body)

            is Node.While  -> getPrimaryReturn(last.body)

            is Node.Do     -> getPrimaryReturn(last.body)

            is Node.For    -> getPrimaryReturn(last.body)

            else           -> error("No primary return!")
        }
    }

    private fun resolveBranchReturns(dataType: DataType, nodes: Nodes) {
        for (node in nodes) {
            if (node is Node.Return && node.dataType != dataType) error("All paths must return the same type @ ${node.location}!")

            return resolveBranchReturns(dataType, node.subNodes)
        }
    }

    private fun `return`(): Node.Return {
        val location = here()

        mustSkip(TokenType.Keyword.RETURN)

        var node: Node? = null

        if (!skip(TokenType.Symbol.SEMICOLON)) {
            node = expr()

            mustSkip(TokenType.Symbol.SEMICOLON)
        }

        return Node.Return(location, node)
    }

    private fun expression(): Node.Expression {
        val location = here()

        val expr = expr()

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Expression(location, expr)
    }

    private fun expr() =
        assign()

    private fun assign(): Node {
        var expr = or()

        if (matchAny(
                TokenType.Symbol.EQUAL,
                TokenType.Symbol.PLUS_EQUAL,
                TokenType.Symbol.DASH_EQUAL,
                TokenType.Symbol.STAR_EQUAL,
                TokenType.Symbol.SLASH_EQUAL,
                TokenType.Symbol.PERCENT_EQUAL,
                TokenType.Symbol.DOUBLE_AMPERSAND_EQUAL,
                TokenType.Symbol.DOUBLE_PIPE_EQUAL
            )
        ) {
            val (location, symbol) = token

            mustSkip(symbol)

            expr = when (symbol) {
                TokenType.Symbol.EQUAL -> when (expr) {
                    is Node.Variable -> assign(location, expr)

                    is Node.GetIndex -> assignIndex(location, expr)

                    else             -> error("Invalid assign.")
                }

                else                   -> when (expr) {
                    is Node.Variable -> desugarAssign(location, expr, symbol as TokenType.Symbol)

                    is Node.GetIndex -> desugarAssignIndex(location, expr, symbol as TokenType.Symbol)

                    else             -> error("Invalid assign desugar.")
                }
            }
        }

        return expr
    }

    private fun assign(location: Location, expr: Node.Variable): Node.Assign {
        val (_, variable) = memory
            .getVariable(expr)

        val (constant, dataType, address) = variable

        if (constant) error("Variable '${expr.name.value}' cannot be reassigned @ ${expr.location}!")

        val node = or()

        if (dataType != node.dataType) error("Cannot assign a value of type '${node.dataType}' to a variable of type '$dataType' @ $location!")

        return Node.Assign(location, expr, address, node)
    }

    private fun assignIndex(location: Location, expr: Node.GetIndex): Node.SetIndex {
        val (_, variable) = memory
            .getVariable(expr.variable)

        val (constant, dataType, _) = variable

        if (constant) error("Variable '${expr.variable.name.value}' cannot be reassigned @ ${expr.variable.location}!")

        if (dataType !is DataType.Array) TODO("Cannot index!")

        val node = or()

        if (dataType.subType != node.dataType) error("Cannot assign a value of type '${node.dataType}' to array of type '${dataType.subType}' @ $location!")

        return Node.SetIndex(location, expr.variable, expr.indices, node)
    }

    private fun desugarAssign(location: Location, expr: Node.Variable, symbol: TokenType.Symbol): Node.Assign {
        val (_, variable) = memory
            .getVariable(expr)

        val (constant, dataType, address) = variable

        if (constant) error("Variable '${expr.name.value}' cannot be reassigned @ ${expr.location}!")

        val node = or()

        val desugaredOperator = symbol.desugared ?: TODO("Operator not desugared!")

        var operator = Node.Binary.Operator[desugaredOperator]

        if (expr.dataType == DataType.Primitive.INT && node.dataType == DataType.Primitive.INT) {
            operator = operator.intVersion
        }

        val intermediate = Node.Binary(location, operator, expr, node)

        if (dataType != intermediate.dataType) error("Cannot assign a value of type '${intermediate.dataType}' to a variable of type '$dataType' @ $location!")

        return Node.Assign(location, expr, address, intermediate)
    }

    private fun desugarAssignIndex(location: Location, expr: Node.GetIndex, symbol: TokenType.Symbol): Node.SetIndex {
        val (_, variable) = memory
            .getVariable(expr.variable)

        val (constant, dataType, _) = variable

        if (constant) error("Variable '${expr.variable.name.value}' cannot be reassigned @ ${expr.variable.location}!")

        if (dataType !is DataType.Array) TODO("Cannot index!")

        val node = or()

        val desugaredOperator = symbol.desugared ?: TODO("Operator not desugared!")

        var operator = Node.Binary.Operator[desugaredOperator]

        if (expr.dataType == DataType.Primitive.INT && node.dataType == DataType.Primitive.INT) {
            operator = operator.intVersion
        }

        val intermediate = Node.Binary(location, operator, expr, node)

        if (dataType.subType != intermediate.dataType) error("Cannot assign a value of type '${intermediate.dataType}' to array of type '${dataType.subType}' @ $location!")

        return Node.SetIndex(location, expr.variable, expr.indices, intermediate)
    }

    private fun or(): Node {
        var expr = and()

        while (match(TokenType.Symbol.DOUBLE_PIPE)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, Node.Binary.Operator[operator], expr, and())

            expr.dataType
        }

        return expr
    }

    private fun and(): Node {
        var expr = equality()

        while (match(TokenType.Symbol.DOUBLE_AMPERSAND)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, Node.Binary.Operator[operator], expr, equality())

            expr.dataType
        }

        return expr
    }

    private fun equality(): Node {
        var expr = relation()

        while (matchAny(TokenType.Symbol.DOUBLE_EQUAL, TokenType.Symbol.EXCLAMATION_EQUAL)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, Node.Binary.Operator[operator], expr, relation())

            expr.dataType
        }

        return expr
    }

    private fun relation(): Node {
        var expr = additive()

        while (matchAny(
                TokenType.Symbol.LESS,
                TokenType.Symbol.LESS_EQUAL,
                TokenType.Symbol.GREATER,
                TokenType.Symbol.GREATER_EQUAL
            )
        ) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, Node.Binary.Operator[operator], expr, additive())

            expr.dataType
        }

        return expr
    }

    private fun additive(): Node {
        var expr = multiplicative()

        while (matchAny(TokenType.Symbol.PLUS, TokenType.Symbol.DASH)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, Node.Binary.Operator[operator], expr, multiplicative())

            expr.dataType
        }

        return expr
    }

    private fun multiplicative(): Node {
        var expr = unary()

        while (matchAny(TokenType.Symbol.STAR, TokenType.Symbol.SLASH, TokenType.Symbol.PERCENT)) {
            val (location, symbol) = token

            mustSkip(symbol)

            var operator = Node.Binary.Operator[symbol]

            val right = unary()

            if (expr.dataType == DataType.Primitive.INT && right.dataType == DataType.Primitive.INT) {
                operator = operator.intVersion
            }

            expr = Node.Binary(location, operator, expr, right)

            expr.dataType
        }

        return expr
    }

    private fun unary(): Node {
        val expr = if (matchAny(TokenType.Symbol.DASH, TokenType.Symbol.EXCLAMATION)) {
            val (location, operator) = token

            mustSkip(operator)

            Node.Unary(location, Node.Unary.Operator[operator], unary())
        }
        else {
            postfix()
        }

        expr.dataType

        return expr
    }

    private fun postfix(): Node {
        var expr = terminal()

        when {
            match(TokenType.Symbol.LEFT_PAREN)  -> {
                if (expr !is Node.Name) error("Invalid invoke.")

                expr = invoke(expr)
            }

            match(TokenType.Symbol.LEFT_SQUARE) -> {
                if (expr !is Node.Name) error("Invalid index.")

                expr = getIndex(expr.toVariable())
            }
        }

        if (expr is Node.Name) {
            return expr.toVariable()
        }

        return expr
    }

    private fun invoke(name: Node.Name): Node {
        val location = token.location

        val args = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_PAREN)

        if (!skip(TokenType.Symbol.RIGHT_PAREN)) {
            do {
                args += expr()
            }
            while (skip(TokenType.Symbol.COMMA))

            mustSkip(TokenType.Symbol.RIGHT_PAREN)
        }

        val signature = Signature(name, args.map { it.dataType })

        val (dataType, id, isNative) = memory
            .getFunction(signature)

        return if (isNative) {
            val systemID = SystemFunctions[signature].takeIf { it != -1 }
                ?: error("No system function for '$signature' @ ${name.location}!")

            Node.SystemCall(location, name, dataType, systemID, args)
        }
        else {
            Node.Invoke(location, name, dataType, id, args)
        }
    }

    private fun getIndex(target: Node.Variable): Node.GetIndex {
        val location = here()

        val indices = mutableListOf<Node>()

        while (skip(TokenType.Symbol.LEFT_SQUARE)) {
            indices += expr()

            mustSkip(TokenType.Symbol.RIGHT_SQUARE)
        }

        return Node.GetIndex(location, target, indices)
    }

    private fun terminal() = when {
        match<TokenType.Value>()           -> value()

        match<TokenType.Name>()            -> name()

        match(TokenType.Symbol.LEFT_PAREN) -> nested()

        match(TokenType.Symbol.LEFT_BRACE) -> array()

        match(TokenType.Keyword.IF)        -> conditional()

        else                               -> error("Not a terminal (${token.type}).")
    }

    private fun value(): Node.Value {
        val location = here()

        val value = get<TokenType.Value>() ?: error("Not a value.")

        return Node.Value(location, value)
    }

    private fun name(): Node.Name {
        val location = here()

        val name = get<TokenType.Name>() ?: error("Not a name!")

        return Node.Name(location, name)
    }

    private fun label() =
        if (skip(TokenType.Symbol.AT)) name() else null

    private fun createVariable(
        constant: Boolean,
        name: Node.Name,
        dataType: DataType
    ): Node.Variable {
        val location = here()

        memory.addVariable(constant, name.name, dataType, location)

        val (mode, variable) = memory
            .getVariable(name.name, location)

        val (_, type, address) = variable

        return Node.Variable(location, name.name, address, mode, type)
    }

    private fun Node.Name.toVariable(): Node.Variable {
        val (mode, variable) = memory
            .getVariable(name, location)

        val (_, type, address) = variable

        return Node.Variable(location, name, address, mode, type)
    }

    private fun nested(): Node {
        mustSkip(TokenType.Symbol.LEFT_PAREN)

        val node = expr()

        mustSkip(TokenType.Symbol.RIGHT_PAREN)

        return node
    }

    private fun array(): Node.Array {
        val location = here()

        mustSkip(TokenType.Symbol.LEFT_BRACE)

        val elements = mutableListOf<Node>()

        if (!skip(TokenType.Symbol.RIGHT_BRACE)) {
            do {
                elements += expr()
            }
            while (skip(TokenType.Symbol.COMMA))

            mustSkip(TokenType.Symbol.RIGHT_BRACE)
        }

        return Node.Array(location, elements)
    }

    private fun conditional() =
        `if`().toExpr()
}