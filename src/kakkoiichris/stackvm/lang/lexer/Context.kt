package kakkoiichris.stackvm.lang.lexer

import kakkoiichris.stackvm.lang.Source

data class Context(
    val source: Source,
    val row: Int,
    val column: Int,
    val start: Int,
    val length: Int
) {
    /**
     * @param other The end [Context] to encompass
     *
     * @return A new [Context] instance spanning from this token to the other token
     */
    fun withLexeme(lexeme: String) =
        Context(source, row, column, start, lexeme.length)

    fun withEnd(end: Int) =
        Context(source, row, column, start, end - start)

    companion object {
        fun none() = Context(Source("", ""), 0, 0, 0, 0)
    }
}