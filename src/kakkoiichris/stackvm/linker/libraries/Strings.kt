package kakkoiichris.stackvm.linker.libraries

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker

object Strings : Link {
    override val name = "strings"

    override fun open(linker: Linker) {
        linker.addFunction("concat", DataType.string, DataType.string) { _, values ->
            val (b, endB) = linker.scanString(values)
            val (a) = linker.scanString(values, start = endB)

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