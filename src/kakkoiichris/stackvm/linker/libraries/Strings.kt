package kakkoiichris.stackvm.linker.libraries

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker

object Strings : Link {
    override val name = "strings"

    override fun open(linker: Linker) {
        linker.addFunction("concat", "SS", DataType.string, DataType.string) { _, data ->
            val a = data.string(0)
            val b = data.string(1)

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