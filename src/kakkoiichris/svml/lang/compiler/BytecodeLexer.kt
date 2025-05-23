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
package kakkoiichris.svml.lang.compiler

class BytecodeLexer(private val src: String, private val preserveComments: Boolean) : Iterator<Bytecode> {
    private var pos = 0

    override fun hasNext() = pos < src.length

    override fun next(): Bytecode {
        while (!match('\u0000')) {
            if (match(Char::isWhitespace)) {
                skipWhitespace()

                continue
            }

            if (match(';')) {
                if (preserveComments) {
                    return comment()
                }

                skipComment()

                continue
            }

            if (match(Char::isLetter)) {
                return instruction()
            }

            if (match(Char::isDigit)) {
                return value()
            }

            error("Unknown char '${peek()}'!")
        }

        return Bytecode.End
    }

    private fun peek() =
        if (pos < src.length)
            src[pos]
        else
            '\u0000'

    private fun match(char: Char) =
        peek() == char

    private fun match(predicate: (Char) -> Boolean) =
        predicate(peek())

    private fun step() {
        pos++
    }

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

    private fun comment(): Bytecode.Comment {
        step()

        val message = buildString {
            while (!match('\n')) {
                append(peek())
                step()
            }
        }

        return Bytecode.Comment(message)
    }

    private fun skipComment() {
        do {
            step()
        }
        while (!match('\n'))
    }

    private fun instruction(): Bytecode.Instruction {
        val result = buildString {
            do {
                take()
            }
            while (match(Char::isLetter))
        }

        return Bytecode.Instruction.entries
            .firstOrNull { it.name.equals(result, ignoreCase = true) }
            ?: error("Bytecode '$result' is invalid!")
    }

    private fun value(): Bytecode.Value {
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

        val value = result
            .toDoubleOrNull()
            ?: error("Number '$result' is too big!")

        return Bytecode.Value(value)
    }
}