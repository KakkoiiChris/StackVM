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
package kakkoiichris.svml.util

import kakkoiichris.svml.lang.Source
import kakkoiichris.svml.lang.lexer.Context


/**
 * A [RuntimeException] subclass to handle errors specific to the Jolt language.
 *
 * @param message The message of the error
 */
class SVMLError(message: String) : RuntimeException(message)

/**
 * Throws a [SVMLError] that displays an error message, as well as underlining the location within the source code where the error occurred, wrapped in a box.
 *
 * @param message The message of the error
 * @param source The source to get the code line from
 * @param context The location data of the error
 *
 * @throws SVMLError
 */
fun svmlError(message: String, source: Source, context: Context): Nothing {
    val fullErrorMessage = buildString {
        appendLine("SVML Error :: $message!")
        appendLine()
        appendLine("${context.row}| ${source.getLine(context.row)}")

        append(" ".repeat(context.column + (context.row.toString().length) + 1))
        append("$UNDERLINE".repeat(context.length))
    }

    throw SVMLError(fullErrorMessage.wrapBox())
}

/**
 * Throws a [SVMLError] that displays an error message, as well as underlining the location within the source code where the error occurred, wrapped in a box.
 *
 * @param message The message of the error
 * @param context The location data of the error
 *
 * @throws SVMLError
 */
fun svmlError(message: String, context: Context): Nothing=
    svmlError(message, context.source, context)

/**
 * Throws a [SVMLError] that displays an error message, without a source location, wrapped in a box.
 *
 * @param message The message of the error
 *
 * @throws SVMLError
 */
fun svmlError(message: String): Nothing {
    val fullErrorMessage = "SVML Error :: $message!"

    throw SVMLError(fullErrorMessage.wrapBox())
}