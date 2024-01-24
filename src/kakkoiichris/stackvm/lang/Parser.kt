package kakkoiichris.stackvm.lang

class Parser(private val lexer: Lexer, private val optimize: Boolean) : Iterator<Node> {
    private var token = lexer.next()

    override fun hasNext() =
        !match(TokenType.End)

    override fun next() =
        statement()

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
            error("Invalid type '$type'!")
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

        match(TokenType.Keyword.BREAK)                         -> `break`()

        match(TokenType.Keyword.CONTINUE)                      -> `continue`()

        match(TokenType.Keyword.FUNCTION)                      -> function()

        match(TokenType.Keyword.RETURN)                        -> `return`()

        else                                                   -> expression()
    }

    private fun declare(): Node.Declare {
        val location = here()

        val constant = skip(TokenType.Keyword.LET)

        if (!constant) {
            mustSkip(TokenType.Keyword.VAR)
        }

        val name = name()

        mustSkip(TokenType.Symbol.EQUAL)

        val node = expr()

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Declare(location, constant, name, node)
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

            while (!match(TokenType.Symbol.RIGHT_BRACE)) {
                body += statement()
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

        val label = if (skip(TokenType.Symbol.AT)) name() else null

        val body = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_BRACE)

        while (!match(TokenType.Symbol.RIGHT_BRACE)) {
            body += statement()
        }

        mustSkip(TokenType.Symbol.RIGHT_BRACE)

        return Node.While(location, condition, label, body)
    }

    private fun `do`(): Node.Do {
        val location = here()

        mustSkip(TokenType.Keyword.DO)

        val label = if (skip(TokenType.Symbol.AT)) name() else null

        val body = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_BRACE)

        while (!match(TokenType.Symbol.RIGHT_BRACE)) {
            body += statement()
        }

        mustSkip(TokenType.Symbol.RIGHT_BRACE)
        mustSkip(TokenType.Keyword.WHILE)

        val condition = expr()

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Do(location, label, body, condition)
    }

    private fun `break`(): Node.Break {
        val location = here()

        mustSkip(TokenType.Keyword.BREAK)

        val label = if (skip(TokenType.Symbol.AT)) name() else null

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Break(location, label)
    }

    private fun `continue`(): Node.Continue {
        val location = here()

        mustSkip(TokenType.Keyword.CONTINUE)

        val label = if (skip(TokenType.Symbol.AT)) name() else null

        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Continue(location, label)
    }

    private fun function(): Node.Function {
        val location = here()

        mustSkip(TokenType.Keyword.FUNCTION)

        val name = name()

        val params = mutableListOf<Node.Name>()

        if (skip(TokenType.Symbol.LEFT_PAREN) && !skip(TokenType.Symbol.RIGHT_PAREN)) {
            do {
                params += name()
            }
            while (skip(TokenType.Symbol.COMMA))

            mustSkip(TokenType.Symbol.RIGHT_PAREN)
        }

        val body = mutableListOf<Node>()

        if (skip(TokenType.Symbol.EQUAL)) {
            body += expr()

            mustSkip(TokenType.Symbol.SEMICOLON)
        }
        else {
            mustSkip(TokenType.Symbol.LEFT_BRACE)

            while (!match(TokenType.Symbol.RIGHT_BRACE)) {
                body += statement()
            }

            mustSkip(TokenType.Symbol.RIGHT_BRACE)
        }

        return Node.Function(location, name, params, body)
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

        if (match(TokenType.Symbol.EQUAL)) {
            if (expr !is Node.Name) error("Invalid assign.")

            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Assign(location, expr, or())
        }

        return expr
    }

    private fun or(): Node {
        var expr = and()

        while (match(TokenType.Symbol.DOUBLE_PIPE)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, operator as TokenType.Symbol, expr, and())
        }

        return expr
    }

    private fun and(): Node {
        var expr = equality()

        while (match(TokenType.Symbol.DOUBLE_AMPERSAND)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, operator as TokenType.Symbol, expr, equality())
        }

        return expr
    }

    private fun equality(): Node {
        var expr = relation()

        while (matchAny(TokenType.Symbol.DOUBLE_EQUAL, TokenType.Symbol.EXCLAMATION_EQUAL)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, operator as TokenType.Symbol, expr, relation())
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

            expr = Node.Binary(location, operator as TokenType.Symbol, expr, additive())
        }

        return expr
    }

    private fun additive(): Node {
        var expr = multiplicative()

        while (matchAny(TokenType.Symbol.PLUS, TokenType.Symbol.DASH)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, operator as TokenType.Symbol, expr, multiplicative())
        }

        return expr
    }

    private fun multiplicative(): Node {
        var expr = unary()

        while (matchAny(TokenType.Symbol.STAR, TokenType.Symbol.SLASH, TokenType.Symbol.PERCENT)) {
            val (location, operator) = token

            mustSkip(operator)

            expr = Node.Binary(location, operator as TokenType.Symbol, expr, unary())
        }

        return expr
    }

    private fun unary(): Node {
        if (matchAny(TokenType.Symbol.DASH, TokenType.Symbol.EXCLAMATION, TokenType.Symbol.BACK_SLASH)) {
            val (location, operator) = token

            if (operator == TokenType.Symbol.BACK_SLASH) {
                return systemCall()
            }

            mustSkip(operator)

            return Node.Unary(location, operator as TokenType.Symbol, unary())
        }

        return postfix()
    }

    private fun systemCall(): Node.SystemCall {
        val location = here()

        mustSkip(TokenType.Symbol.BACK_SLASH)

        val name = name()

        val args = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_PAREN)

        if (!skip(TokenType.Symbol.RIGHT_PAREN)) {
            do {
                args += expr()
            }
            while (skip(TokenType.Symbol.COMMA))

            mustSkip(TokenType.Symbol.RIGHT_PAREN)
        }

        return Node.SystemCall(location, name, args)
    }

    private fun postfix(): Node {
        var expr = terminal()

        if (match(TokenType.Symbol.LEFT_PAREN)) {
            if (expr !is Node.Name) error("Invalid invoke.")

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

            expr = Node.Invoke(location, expr, args)
        }

        return expr
    }

    private fun terminal() = when {
        match<TokenType.Value>()           -> value()

        match<TokenType.Name>()            -> name()

        match(TokenType.Symbol.LEFT_PAREN) -> nested()

        match(TokenType.Keyword.IF)        -> conditional()

        else                               -> error("Not a terminal.")
    }

    private fun value(): Node.Value {
        val location = here()

        val value = get<TokenType.Value>() ?: error("Not a value.")

        return Node.Value(location, value)
    }

    private fun name(): Node.Name {
        val location = here()

        val name = get<TokenType.Name>() ?: error("Not a name.")

        return Node.Name(location, name)
    }

    private fun nested(): Node {
        mustSkip(TokenType.Symbol.LEFT_PAREN)

        val node = expr()

        mustSkip(TokenType.Symbol.RIGHT_PAREN)

        return node
    }

    private fun conditional() =
        `if`().toExpr()
}