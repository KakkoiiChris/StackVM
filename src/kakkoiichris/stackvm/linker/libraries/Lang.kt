package kakkoiichris.stackvm.linker.libraries

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker

object Lang : Link {
    override val name = "lang"

    override fun open(linker: Linker) {
        linker.addFunction("toFloat", "I", DataType.Primitive.INT) { _, data ->
            val i = data.int()

            listOf(i.toDouble())
        }

        linker.addFunction("toInt", "F", DataType.Primitive.FLOAT) { _, data ->
            val f = data.float()

            listOf(f)
        }

        linker.addFunction("toInt", "C", DataType.Primitive.CHAR) { _, data ->
            val c = data.char()

            listOf(c.code.toDouble())
        }

        linker.addFunction("toChar", "I", DataType.Primitive.INT) { _, data ->
            val i = data.int()

            listOf(i.toDouble())
        }

        linker.addFunction("sleep", "F", DataType.Primitive.FLOAT) { _, data ->
            val time = data.float()

            Thread.sleep((time * 1000).toLong())

            Linker.void
        }

        linker.addFunction("exit", "I", DataType.Primitive.INT) { cpu, data ->
            val code = data.int()

            cpu.result = code.toDouble()
            cpu.running = false

            Linker.void
        }
    }

    override fun close(linker: Linker) = Unit
}