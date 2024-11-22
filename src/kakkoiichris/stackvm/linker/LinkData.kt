package kakkoiichris.stackvm.linker

import kakkoiichris.stackvm.util.bool

class LinkData private constructor(val arguments: MutableMap<Int, Any>) {
    fun bool(i: Int) =
        arguments[i] as Boolean

    fun float(i: Int) =
        arguments[i] as Double

    fun int(i: Int) =
        arguments[i] as Int

    fun char(i: Int) =
        arguments[i] as Char

    fun string(i: Int) =
        arguments[i] as String

    companion object {
        fun parse(values: Values, format: String): LinkData {
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