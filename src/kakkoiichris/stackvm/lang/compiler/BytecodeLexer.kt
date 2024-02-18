package kakkoiichris.stackvm.lang.compiler

class BytecodeLexer(private val src: String) : Iterator<Bytecode> {
    private var pos = 0

    override fun hasNext() = pos < src.length

    override fun next(): Bytecode {
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

        return Bytecode.End
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

    private fun keyword(): Bytecode {
        val result = buildString {
            do {
                take()
            }
            while (match(Char::isLetter))
        }

        return Bytecode.Instruction.entries.first { it.name.equals(result, ignoreCase = true) }
    }

    private fun value(): Bytecode {
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

        val value = result.toDoubleOrNull() ?: error("Number too big!")

        return Bytecode.Value(value)
    }
}