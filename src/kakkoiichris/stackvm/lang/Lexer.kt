package kakkoiichris.stackvm.lang

class Lexer(private val src: String) : Iterator<Token> {
    private var pos = 0
    private var row = 1
    private var col = 1

    override fun hasNext() = pos <= src.length

    override fun next(): Token {
        while (!match('\u0000')) {
            if (match(Char::isWhitespace)) {
                skipWhitespace()

                continue
            }

            if (match('#')) {
                skipComment()

                continue
            }

            if (match(Char::isLetter)) {
                return word()
            }

            if (match(Char::isDigit)) {
                return value()
            }

            return symbol()
        }

        return Token(here(), TokenType.End)
    }

    private fun here() = Location(row, col)

    private fun peek() = if (pos < src.length) src[pos] else '\u0000'

    private fun match(char: Char) =
        peek() == char

    private fun match(predicate: (Char) -> Boolean) =
        predicate(peek())

    private fun step() {
        if (match('\n')) {
            row++
            col = 1
        }
        else {
            col++
        }

        pos++
    }

    private fun skip(char: Char) =
        if (match(char)) {
            step()

            true
        }
        else false

    private fun StringBuilder.take() {
        append(peek())

        step()
    }

    private fun skipWhitespace() {
        do {
            step()
        }
        while (match(Char::isWhitespace))
    }

    private fun skipComment() {
        do {
            step()
        }
        while (!match('\n'))
    }

    private fun word(): Token {
        val location = here()

        val result = buildString {
            do {
                take()
            }
            while (match(Char::isLetter))
        }

        if (result.equals("true", ignoreCase = true)) {
            return Token(location, TokenType.Value(1F))
        }

        if (result.equals("false", ignoreCase = true)) {
            return Token(location, TokenType.Value(0F))
        }

        val keyword = TokenType.Keyword.entries.firstOrNull { it.name.equals(result, ignoreCase = true) }

        if (keyword != null) {
            return Token(location, keyword)
        }

        return Token(location, TokenType.Name(result))
    }

    private fun value(): Token {
        val location = here()

        val result = buildString {
            do {
                take()
            }
            while (match(Char::isDigit))

            if (match('.')) {
                do {
                    take()
                }
                while (match(Char::isDigit))
            }
        }

        val value = result.toFloatOrNull() ?: error("Number too big!")

        return Token(location, TokenType.Value(value))
    }

    private fun symbol(): Token {
        val location = here()

        val symbol = when {
            skip('+') -> TokenType.Symbol.PLUS

            skip('-') -> TokenType.Symbol.DASH

            skip('*') -> TokenType.Symbol.STAR

            skip('/') -> TokenType.Symbol.SLASH

            skip('%') -> TokenType.Symbol.PERCENT

            skip('<') -> when {
                skip('=') -> TokenType.Symbol.LESS_EQUAL

                else      -> TokenType.Symbol.LESS
            }

            skip('>') -> when {
                skip('=') -> TokenType.Symbol.GREATER_EQUAL

                else      -> TokenType.Symbol.GREATER
            }

            skip('=') -> when {
                skip('=') -> TokenType.Symbol.DOUBLE_EQUAL

                else      -> TokenType.Symbol.EQUAL
            }

            skip('!') -> when {
                skip('=') -> TokenType.Symbol.EXCLAMATION_EQUAL

                else      -> TokenType.Symbol.EXCLAMATION
            }

            skip('&') -> when {
                skip('&') -> TokenType.Symbol.DOUBLE_AMPERSAND

                else      -> error("No single ampersand.")
            }

            skip('|') -> when {
                skip('|') -> TokenType.Symbol.DOUBLE_PIPE

                else      -> error("No single pipe.")
            }

            skip('(') -> TokenType.Symbol.LEFT_PAREN

            skip(')') -> TokenType.Symbol.RIGHT_PAREN

            skip('{') -> TokenType.Symbol.LEFT_BRACE

            skip('}') -> TokenType.Symbol.RIGHT_BRACE

            skip(';') -> TokenType.Symbol.SEMICOLON

            skip(',') -> TokenType.Symbol.COMMA

            else      -> error("Unknown symbol '${peek()}'!")
        }

        return Token(location, symbol)
    }
}