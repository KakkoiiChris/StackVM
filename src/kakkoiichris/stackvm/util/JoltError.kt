/*      ___  _______  ___    _______
 *     |   ||       ||   |  |       |
 *     |   ||   _   ||   |  |_     _|
 *     |   ||  | |  ||   |    |   |
 *  ___|   ||  |_|  ||   |___ |   |
 * |       ||       ||       ||   |
 * |_______||_______||_______||___|
 *         SCRIPTING LANGUAGE
 */
package kakkoiichris.stackvm.util

import kakkoiichris.stackvm.lang.Source
import kakkoiichris.stackvm.lang.lexer.Context


/**
 * A [RuntimeException] subclass to handle errors specific to the Jolt language.
 *
 * @param message The message of the error
 */
class SVMLError(message: String) : RuntimeException(message)

/**
 * Throws a [SVMLError] that displays an error message, as well as underlining the location within the source code that the error occurred, wrapped in a box.
 *
 * @param message The message of the error
 * @param source The source to get the code line from
 * @param context The location data of the error
 *
 * @throws SVMLError
 */
fun svmlError(message: String, source: Source, context: Context): Nothing {
    val fullErrorMessage = buildString {
        appendLine("Jolt Error $JOLT $message!")
        appendLine()
        appendLine("${context.row}| ${source.getLine(context.row)}")

        append(" ".repeat(context.column + (context.row.toString().length) + 1))
        append("$UNDERLINE".repeat(context.length))
    }

    throw SVMLError(fullErrorMessage.wrapBox())
}