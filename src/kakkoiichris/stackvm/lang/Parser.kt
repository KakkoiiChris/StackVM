package stackvm.lang

class Parser(val lexer: Lexer) : Iterator<Node> {
    val token = lexer.next()

    override fun hasNext() =
        lexer.hasNext()

    override fun next() =
        statement()

    private fun match(vararg types: TokenType) =
        types.any { it == token.type }

    private fun statement() = when {
        match(Keyword.IF)       -> `if`()

        match(Keyword.WHILE)    -> `while`()

        match(Keyword.BREAK)    -> `break`()

        match(Keyword.CONTINUE) -> `continue`()

        else                    -> expression()
    }

    private fun ifStmt():If
}