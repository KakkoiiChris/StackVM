package kakkoiichris.stackvm.linker

import kakkoiichris.stackvm.util.bool

class LinkData(val values: Values) {
    private var scanIndex = 0

    fun bool() =
        values[scanIndex++].bool

    fun float() =
        values[scanIndex++]

    fun int() =
        values[scanIndex++].toInt()

    fun char() =
        values[scanIndex++].toInt().toChar()

    fun string(): String {
        val (string, end) = scanString(values, scanIndex)

        scanIndex = end

        return string
    }

    companion object {
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