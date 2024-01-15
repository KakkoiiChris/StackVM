package kakkoiichris.stackvm.lang

class Parser(private val lexer: Lexer, private val optimize:Boolean) : Iterator<Node> {
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
            error("Invalid type")
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
        matchAny(TokenType.Keyword.IF)       -> `if`()

        matchAny(TokenType.Keyword.WHILE)    -> `while`()

        matchAny(TokenType.Keyword.BREAK)    -> `break`()

        matchAny(TokenType.Keyword.CONTINUE) -> `continue`()

        else                                 -> expression()
    }

    private fun `if`(): Node.If {
        val location = here()

        mustSkip(TokenType.Keyword.IF)

        val condition = expr()

        val body = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_BRACE)

        while (!match(TokenType.Symbol.RIGHT_BRACE)) {
            body += statement()
        }

        mustSkip(TokenType.Symbol.RIGHT_BRACE)

        return Node.If(location, condition, body)
    }

    private fun `while`(): Node.While {
        val location = here()

        mustSkip(TokenType.Keyword.WHILE)

        val condition = expr()

        val body = mutableListOf<Node>()

        mustSkip(TokenType.Symbol.LEFT_BRACE)

        while (!match(TokenType.Symbol.RIGHT_BRACE)) {
            body += statement()
        }

        mustSkip(TokenType.Symbol.RIGHT_BRACE)

        return Node.While(location, condition, body)
    }

    private fun `break`(): Node.Break {
        val location = here()

        mustSkip(TokenType.Keyword.BREAK)
        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Break(location)
    }

    private fun `continue`(): Node.Continue {
        val location = here()

        mustSkip(TokenType.Keyword.CONTINUE)
        mustSkip(TokenType.Symbol.SEMICOLON)

        return Node.Continue(location)
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
        if (matchAny(TokenType.Symbol.DASH, TokenType.Symbol.EXCLAMATION)) {
            val (location, operator) = token

            mustSkip(operator)

            return Node.Unary(location, operator as TokenType.Symbol, unary())
        }

        return terminal()
    }

    private fun terminal() = when {
        match<TokenType.Value>()           -> value()

        match<TokenType.Name>()            -> name()

        match(TokenType.Symbol.LEFT_PAREN) -> nested()

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
}