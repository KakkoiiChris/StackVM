package kakkoiichris.stackvm.lang.lexer

import kakkoiichris.stackvm.lang.parser.DataType

class Lexer(private val file: String, private val src: String) : Iterator<Token> {
    companion object {
        private const val NUL = '\u0000'
    }

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

            if (match("//")) {
                skipLineComment()

                continue
            }

            if (match("/*")) {
                skipBlockComment()

                continue
            }

            if (match(Char::isLetter)) {
                return word()
            }

            if (match(Char::isDigit)) {
                return number()
            }

            if (match('\'')) {
                return char()
            }

            if (match('"')) {
                return string()
            }

            return symbol()
        }

        return Token(here(), TokenType.End)
    }

    private fun here() = Location(file, row, col)

    private fun peek(offset: Int = 0) = if (pos + offset < src.length)
        src[pos + offset]
    else
        NUL

    private fun look(length: Int) = buildString {
        repeat(length) { i -> append(peek(i)) }
    }

    private fun match(char: Char) =
        peek() == char

    private fun match(predicate: (Char) -> Boolean) =
        predicate(peek())

    private fun match(string: String) =
        look(string.length) == string

    private fun step(amount: Int = 1) {
        repeat(amount) {
            if (match('\n')) {
                row++
                col = 1
            }
            else {
                col++
            }

            pos++
        }
    }

    private fun skip(char: Char) =
        if (match(char)) {
            step()

            true
        }
        else false

    private fun skip(string: String) =
        if (match(string)) {
            step(string.length)

            true
        }
        else false

    private fun mustSkip(char: Char) {
        if (!skip(char)) {
            error("Illegal char '${peek()}' @ ${here()}!")
        }
    }

    private fun mustSkip(string: String) {
        if (!skip(string)) {
            error("Illegal string '${look(string.length)}' @ ${here()}!")
        }
    }

    private fun StringBuilder.take() {
        append(peek())

        step()
    }

    fun get(): Char {
        val char = peek()

        step()

        return char
    }

    private fun skipWhitespace() {
        do {
            step()
        }
        while (match(Char::isWhitespace))
    }

    private fun skipLineComment() {
        mustSkip("//")

        while (!(skip('\n') || match(NUL))) {
            step()
        }
    }

    private fun skipBlockComment() {
        mustSkip("/*")

        while (!(skip("*/") || match(NUL))) {
            step()
        }
    }

    private fun word(): Token {
        val location = here()

        val result = buildString {
            do {
                take()
            }
            while (match(Char::isLetterOrDigit) || match('_'))
        }

        if (result.equals("true", ignoreCase = true)) {
            return Token(location, TokenType.Value(1F, DataType.Primitive.BOOL))
        }

        if (result.equals("false", ignoreCase = true)) {
            return Token(location, TokenType.Value(0F, DataType.Primitive.BOOL))
        }

        val keyword = TokenType.Keyword.entries.firstOrNull { it.name.equals(result, ignoreCase = true) }

        if (keyword != null) {
            return Token(location, keyword)
        }

        return Token(location, TokenType.Name(result))
    }

    private fun number(): Token {
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

            if (match('E') || match('e')) {
                take()

                do {
                    take()
                }
                while (match(Char::isDigit))
            }
        }

        if (result.contains("[Ee.]".toRegex())) {
            val value = result.toFloatOrNull() ?: error("Floating point number '$result' is out of bounds @ $location!")

            return Token(location, TokenType.Value(value, DataType.Primitive.FLOAT))
        }

        val value = result.toIntOrNull() ?: error("Integer number '$result' is out of bounds @ $location!")

        return Token(location, TokenType.Value(value.toFloat(), DataType.Primitive.INT))
    }

    private fun hex(length: Int) =
        buildString { repeat(length) { take() } }
            .toInt(16)
            .toChar()

    private fun getTextChar(delimiter: Char) = if (skip('\\')) when {
        skip('0')       -> '\u0000'

        skip('a')       -> '\u0007'

        skip('b')       -> '\b'

        skip('f')       -> '\u000C'

        skip('n')       -> '\n'

        skip('r')       -> '\r'

        skip('t')       -> '\t'

        skip('v')       -> '\u000B'

        skip('\\')      -> '\\'

        skip(delimiter) -> delimiter

        skip('x')       -> hex(2)

        skip('u')       -> hex(4)

        else            -> error("Illegal character escape sequence '\\${peek()}' @ ${here()}!")
    }
    else {
        get()
    }

    private fun char(): Token {
        val location = here()

        mustSkip('\'')

        val result = getTextChar('\'')

        mustSkip('\'')

        val value = result.code.toFloat()

        return Token(location, TokenType.Value(value, DataType.Primitive.CHAR))
    }

    private fun string(): Token {
        val location = here()

        mustSkip('"')

        val value = buildString {
            while (!skip('"')) {
                append(getTextChar('"'))
            }
        }

        return Token(location, TokenType.String(value))
    }

    private fun symbol(): Token {
        val location = here()

        val symbol = when {
            skip('+') -> when {
                skip('=') -> TokenType.Symbol.PLUS_EQUAL

                else      -> TokenType.Symbol.PLUS
            }

            skip('-') -> when {
                skip('=') -> TokenType.Symbol.DASH_EQUAL

                else      -> TokenType.Symbol.DASH
            }

            skip('*') -> when {
                skip('=') -> TokenType.Symbol.STAR_EQUAL

                else      -> TokenType.Symbol.STAR
            }

            skip('/') -> when {
                skip('=') -> TokenType.Symbol.SLASH_EQUAL

                else      -> TokenType.Symbol.SLASH
            }

            skip('%') -> when {
                skip('=') -> TokenType.Symbol.PERCENT_EQUAL

                else      -> TokenType.Symbol.PERCENT
            }

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
                skip('&') -> when {
                    skip('=') -> TokenType.Symbol.DOUBLE_AMPERSAND_EQUAL

                    else      -> TokenType.Symbol.DOUBLE_AMPERSAND
                }

                else      -> error("Unknown symbol '&' @ $location!")
            }

            skip('|') -> when {
                skip('|') -> when {
                    skip('=') -> TokenType.Symbol.DOUBLE_PIPE_EQUAL

                    else      -> TokenType.Symbol.DOUBLE_PIPE
                }

                else      -> error("Unknown symbol '|' @ $location!")
            }

            skip('(') -> TokenType.Symbol.LEFT_PAREN

            skip(')') -> TokenType.Symbol.RIGHT_PAREN

            skip('{') -> TokenType.Symbol.LEFT_BRACE

            skip('}') -> TokenType.Symbol.RIGHT_BRACE

            skip('[') -> TokenType.Symbol.LEFT_SQUARE

            skip(']') -> TokenType.Symbol.RIGHT_SQUARE

            skip(';') -> TokenType.Symbol.SEMICOLON

            skip(',') -> TokenType.Symbol.COMMA

            skip('@') -> TokenType.Symbol.AT

            skip(':') -> TokenType.Symbol.COLON

            skip('#') -> TokenType.Symbol.POUND

            else      -> error("Unknown character '${peek()}' @ $location!")
        }

        return Token(location, symbol)
    }
}