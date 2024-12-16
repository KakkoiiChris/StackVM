package kakkoiichris.stackvm.lang.lexer

data class Context(
    val name: String,
    val row: Int,
    val column: Int,
    val start: Int,
    val end: Int
) {
    /**
     * @return The horizontal length of this token
     */
    val length get() = end - start

    /**
     * @param other The end [Context] to encompass
     *
     * @return A new [Context] instance spanning from this token to the other token
     */
    operator fun rangeTo(other: Context) =
        Context(name, row, column, start, other.end - 1)

    companion object {
        val none = Context("", 0, 0, 0, 0)
    }
}