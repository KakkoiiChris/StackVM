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
package kakkoiichris.svml.linker.libraries

import kakkoiichris.svml.lang.parser.DataType
import kakkoiichris.svml.linker.Link
import kakkoiichris.svml.linker.Linker

object Strings : Link {
    override val name = "strings"

    override fun open(linker: Linker) {
        linker.addFunction("concat", "SS", DataType.string, DataType.string) { _, data ->
            val a = data.string()
            val b = data.string()

            val result = a + b

            val length = result.length

            result.toCharArray()
                .map { it.code.toDouble() }
                .toMutableList()
                .apply { addFirst(length.toDouble()) }
        }
    }

    override fun close(linker: Linker) = Unit
}