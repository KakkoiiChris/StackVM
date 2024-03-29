package kakkoiichris.stackvm.linker.libraries

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker

object Lang : Link {
    override val name = "lang"

    override fun open(linker: Linker) {
        Linker.addFunction("toFloat", DataType.Primitive.INT) { _, args ->
            val (i) = args

            listOf(i)
        }

        Linker.addFunction("toInt", DataType.Primitive.FLOAT) { _, args ->
            val (i) = args

            listOf(i)
        }

        Linker.addFunction("toInt", DataType.Primitive.CHAR) { _, args ->
            val (i) = args

            listOf(i)
        }

        Linker.addFunction("toChar", DataType.Primitive.INT) { _, args ->
            val (i) = args

            listOf(i)
        }

        Linker.addFunction("exit", DataType.Primitive.INT) { cpu, args ->
            val (code) = args

            cpu.result = code
            cpu.running = false

            Linker.void
        }
    }

    override fun close(linker: Linker) = Unit
}