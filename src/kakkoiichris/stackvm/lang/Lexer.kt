package stackvm.lang

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
                return keyword()
            }

            if (match(Char::isDigit)) {
                return value()
            }

            return symbol()
        }

        return Token(row, col, End)
    }

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

    private fun keyword(): Token {
        val row = row
        val col = col

        val result = buildString {
            do {
                take()
            }
            while (match(Char::isLetter))
        }

        val keyword = Keyword.values().first { it.name.equals(result, ignoreCase = true) }

        return Token(row, col, keyword)
    }

    private fun value(): Token {
        val row = row
        val col = col

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

        return Token(row, col, Value(value))
    }

    private fun symbol(): Token {
        val row = row
        val col = col

        val symbol = when {
            skip('+') -> Symbol.PLUS

            skip('-') -> Symbol.DASH

            skip('*') -> Symbol.STAR

            skip('/') -> Symbol.SLASH

            skip('%') -> Symbol.PERCENT

            skip('<') -> when {
                skip('=') -> Symbol.LESS_EQUAL

                else      -> Symbol.LESS
            }

            skip('>') -> when {
                skip('=') -> Symbol.GREATER_EQUAL

                else      -> Symbol.GREATER
            }

            skip('=') -> when {
                skip('=') -> Symbol.DOUBLE_EQUAL

                else      -> Symbol.EQUAL
            }

            skip('!') -> when {
                skip('=') -> Symbol.EXCLAMATION_EQUAL

                else      -> Symbol.EXCLAMATION
            }

            skip('&') -> Symbol.AND

            skip('|') -> Symbol.PIPE

            skip('(') -> Symbol.LEFT_PAREN

            skip(')') -> Symbol.RIGHT_PAREN

            skip('{') -> Symbol.LEFT_BRACE

            skip('}') -> Symbol.RIGHT_BRACE

            skip(';') -> Symbol.SEMICOLON

            else      -> error("Unknown symbol '${peek()}'!")
        }

        return Token(row, col, symbol)
    }
}