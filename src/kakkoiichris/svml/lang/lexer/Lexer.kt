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
package kakkoiichris.svml.lang.lexer

import kakkoiichris.svml.lang.Source
import kakkoiichris.svml.lang.parser.DataType
import kakkoiichris.svml.util.svmlError

class Lexer(private val source: Source) : Iterator<Token> {
    companion object {
        private const val NUL = '\u0000'

        private val literals = mapOf(
            "true" to TokenType.Value(1.0, DataType.Primitive.BOOL),
            "false" to TokenType.Value(0.0, DataType.Primitive.BOOL)
        )
    }

    private var pos = 0
    private var row = 1
    private var col = 1

    private val lexeme = StringBuilder()

    override fun hasNext() = pos <= source.text.length

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

            lexeme.clear()

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

    private fun here() = Context(source, row, col, pos, 1)

    private fun peek(offset: Int = 0) = if (pos + offset < source.text.length)
        source.text[pos + offset]
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
            lexeme.append(peek())

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
            svmlError("Illegal char '${peek()}'", source, here())
        }
    }

    private fun mustSkip(string: String) {
        if (!skip(string)) {
            svmlError("Illegal string '${look(string.length)}'", source, here())
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
        var context = here()

        val result = buildString {
            do {
                take()
            }
            while (match(Char::isLetterOrDigit) || match('_'))
        }

        val lexeme = lexeme.toString()

        context = context.withLexeme(lexeme)

        if (result.equals("true", ignoreCase = true)) {
            return Token(context, TokenType.Value(1.0, DataType.Primitive.BOOL))
        }

        if (result.equals("false", ignoreCase = true)) {
            return Token(context, TokenType.Value(0.0, DataType.Primitive.BOOL))
        }

        val literal = literals[result]

        if (literal != null) {
            return Token(context, literal)
        }

        val keyword = TokenType.Keyword.entries.firstOrNull { it.name.equals(result, ignoreCase = true) }

        if (keyword != null) {
            return Token(context, keyword)
        }

        return Token(context, TokenType.Name(result))
    }

    private fun number(): Token {
        var context = here()

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

        val lexeme = lexeme.toString()

        context = context.withLexeme(lexeme)

        if (result.contains("[Ee.]".toRegex())) {
            val value = result.toDoubleOrNull() ?: svmlError("Floating point number '$result' is out of bounds", source, context)

            return Token(context, TokenType.Value(value, DataType.Primitive.FLOAT))
        }

        val value = result.toIntOrNull() ?: svmlError("Integer number '$result' is out of bounds", source, context)

        return Token(context, TokenType.Value(value.toDouble(), DataType.Primitive.INT))
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

        else            -> svmlError("Illegal character escape sequence '\\${peek()}'", source, here())
    }
    else {
        get()
    }

    private fun char(): Token {
        var context = here()

        mustSkip('\'')

        val result = getTextChar('\'')

        mustSkip('\'')

        val lexeme = lexeme.toString()

        context = context.withLexeme(lexeme)

        val value = result.code.toDouble()

        return Token(context, TokenType.Value(value, DataType.Primitive.CHAR))
    }

    private fun string(): Token {
        var context = here()

        mustSkip('"')

        val value = buildString {
            while (!skip('"')) {
                append(getTextChar('"'))
            }
        }

        val lexeme = lexeme.toString()

        context = context.withLexeme(lexeme)

        return Token(context, TokenType.String(value))
    }

    private fun symbol(): Token {
        var context = here()

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

                else      -> svmlError("Unknown symbol '&'", source, here())
            }

            skip('|') -> when {
                skip('|') -> when {
                    skip('=') -> TokenType.Symbol.DOUBLE_PIPE_EQUAL

                    else      -> TokenType.Symbol.DOUBLE_PIPE
                }

                else      -> svmlError("Unknown symbol '|'", source, here())
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

            else      -> svmlError("Unknown character '${peek()}'", source, here())
        }

        val lexeme = lexeme.toString()

        context = context.withLexeme(lexeme)

        return Token(context, symbol)
    }
}