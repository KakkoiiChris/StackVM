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
package kakkoiichris.svml.linker

import kakkoiichris.svml.util.bool

class LinkData private constructor(private val arguments: MutableMap<Int, Any>) {
    private var i = 0

    fun bool() =
        arguments[i++] as Boolean

    fun float() =
        arguments[i++] as Double

    fun int() =
        arguments[i++] as Int

    fun char() =
        arguments[i++] as Char

    fun string() =
        arguments[i++] as String

    companion object {
        fun parse(format: String, values: Values): LinkData {
            val arguments = mutableMapOf<Int, Any>()

            val parseOrder = format.uppercase().withIndex().reversed()

            var j = 0

            for ((i, c) in parseOrder) {
                arguments[i] = when (c) {
                    'B'  -> values[j++].bool

                    'F'  -> values[j++]

                    'I'  -> values[j++].toInt()

                    'C'  -> values[j++].toInt().toChar()

                    'S'  -> {
                        val (string, end) = scanString(values, j)

                        j = end

                        string
                    }

                    else -> TODO()
                }
            }

            return LinkData(arguments)
        }

        private fun scanString(values: List<Double>, start: Int = 0): StringScan {
            var i = start

            val size = values[i++]

            val result = buildString {
                repeat(size.toInt()) {
                    append(values[i++].toInt().toChar())
                }
            }

            return StringScan(result, i)
        }

        private data class StringScan(val string: String, val end: Int)
    }
}