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

import kotlin.math.absoluteValue

fun Double.truncate() =
    if (this - toInt() == 0.0)
        toInt().toString()
    else
        toString()

val Double.bool get() = this != 0.0

val Boolean.float get() = if (this) 1.0 else 0.0

fun Int.toAddress() =
    if (this < 0)
        "-${absoluteValue.toString().uppercase()}"
    else
        toString().uppercase()