package kakkoiichris.stackvm.asm

class ASMLexer(private val src: String) : Iterator<ASMToken> {
    private var pos = 0

    override fun hasNext() = pos < src.length

    override fun next(): ASMToken {
        while (!match('\u0000')) {
            if (match(Char::isWhitespace)) {
                skipWhitespace()

                continue
            }

            if (match(';')) {
                skipComment()

                continue
            }

            if (match(Char::isLetter)) {
                return keyword()
            }

            if (match(Char::isDigit)) {
                return value()
            }

            error("Unknown char '${peek()}'!")
        }

        return ASMToken.End
    }

    private fun peek() = if (pos < src.length) src[pos] else '\u0000'

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

    private fun skipComment() {
        do {
            step()
        }
        while (!match('\n'))
    }

    private fun keyword(): ASMToken {
        val result = buildString {
            do {
                take()
            }
            while (match(Char::isLetter))
        }

        return ASMToken.Instruction.entries.first { it.name.equals(result, ignoreCase = true) }
    }

    private fun value(): ASMToken {
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

        return ASMToken.Value(value)
    }
}