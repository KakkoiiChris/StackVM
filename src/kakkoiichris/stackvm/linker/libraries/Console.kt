package kakkoiichris.stackvm.linker.libraries

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker
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
            TODO("readChar not implemented")
        }

        linker.addFunction("read") { _, _ ->
            val text = input.next().toCharArray().map { it.code.toDouble() }

            text.toMutableList().apply { addFirst(text.size.toDouble()) }
        }

        linker.addFunction("readLine") { _, _ ->
            val text = input.nextLine().toCharArray().map { it.code.toDouble() }

            text.toMutableList().apply { addFirst(text.size.toDouble()) }
        }

        linker.addFunction("write", "B", DataType.Primitive.BOOL) { _, data ->
            val b = data.bool()

            print(b)

            Linker.void
        }

        linker.addFunction("write", "I", DataType.Primitive.INT) { _, data ->
            val i = data.int()

            print(i)

            Linker.void
        }

        linker.addFunction("write", "F", DataType.Primitive.FLOAT) { _, data ->
            val f = data.float()

            print(f.truncate())

            Linker.void
        }

        linker.addFunction("write", "C", DataType.Primitive.CHAR) { _, data ->
            val c = data.char()

            print(c)

            Linker.void
        }
    }

    override fun close(linker: Linker) = input.close()
}