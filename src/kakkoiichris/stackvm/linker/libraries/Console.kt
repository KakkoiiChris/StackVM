package kakkoiichris.stackvm.linker.libraries

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker
import kakkoiichris.stackvm.util.bool
import kakkoiichris.stackvm.util.float
import kakkoiichris.stackvm.util.truncate
import java.util.*

object Console : Link {
    private val input = Scanner(System.`in`)

    override val name = "console"

    override fun open(library: Linker) {
        Linker.addFunction("readBool") { _, _ ->
            listOf(input.nextBoolean().float)
        }

        Linker.addFunction("readFloat") { _, _ ->
            listOf(input.nextDouble())
        }

        Linker.addFunction("readInt") { _, _ ->
            listOf(input.nextInt().toDouble())
        }

        Linker.addFunction("readChar") { _, _ ->
            Linker.void//TODO readChar
        }

        Linker.addFunction("read") { _, _ ->
            val text = input.next().toCharArray().map { it.code.toDouble() }

            text.toMutableList().apply { add(0, text.size.toDouble()) }
        }

        Linker.addFunction("readLine") { _, _ ->
            val text = input.nextLine().toCharArray().map { it.code.toDouble() }

            text.toMutableList().apply { add(0, text.size.toDouble()) }
        }

        Linker.addFunction("write", DataType.Primitive.BOOL) { _, values ->
            val (n) = values

            print(n.bool)

            Linker.void
        }

        Linker.addFunction("write", DataType.Primitive.INT) { _, values ->
            val (n) = values

            print(n.toInt())

            Linker.void
        }

        Linker.addFunction("write", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            print(n.truncate())

            Linker.void
        }

        Linker.addFunction("write", DataType.Primitive.CHAR) { _, values ->
            val (n) = values

            print(n.toInt().toChar())

            Linker.void
        }
    }

    override fun close(library: Linker) = input.close()
}