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

    override fun open(linker: Linker) {
        linker.addFunction("readBool") { _, _ ->
            listOf(input.nextBoolean().float)
        }

        linker.addFunction("readFloat") { _, _ ->
            listOf(input.nextDouble())
        }

        linker.addFunction("readInt") { _, _ ->
            listOf(input.nextInt().toDouble())
        }

        linker.addFunction("readChar") { _, _ ->
            Linker.void//TODO readChar
        }

        linker.addFunction("read") { _, _ ->
            val text = input.next().toCharArray().map { it.code.toDouble() }

            text.toMutableList().apply { addFirst(text.size.toDouble()) }
        }

        linker.addFunction("readLine") { _, _ ->
            val text = input.nextLine().toCharArray().map { it.code.toDouble() }

            text.toMutableList().apply { addFirst(text.size.toDouble()) }
        }

        linker.addFunction("write", DataType.Primitive.BOOL) { _, values ->
            val (n) = values

            print(n.bool)

            Linker.void
        }

        linker.addFunction("write", DataType.Primitive.INT) { _, values ->
            val (n) = values

            print(n.toInt())

            Linker.void
        }

        linker.addFunction("write", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            print(n.truncate())

            Linker.void
        }

        linker.addFunction("write", DataType.Primitive.CHAR) { _, values ->
            val (n) = values

            print(n.toInt().toChar())

            Linker.void
        }
    }

    override fun close(linker: Linker) = input.close()
}