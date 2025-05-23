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

    fun getCodeStamp()=
        "${source.name} (Row $row, Column $column)"

    companion object {
        fun none() = Context(Source("", ""), 0, 0, 0, 0)
    }
}