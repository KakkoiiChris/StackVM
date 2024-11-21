package kakkoiichris.stackvm.linker.libraries.gfx

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker
import kakkoiichris.stackvm.util.float

object Graphics : Link {
    private val displays = mutableListOf<Display>()

    override val name = "graphics"

    override fun open(linker: Linker) {
        linker.addFunction(
            "gfxCreate",
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.string
        ) { _, values ->
            var (title, end) = linker.scanString(values)

            val height = values[end++].toInt()
            val width = values[end].toInt()

            displays += Display(width, height, title)

            listOf(displays.size.toDouble() - 1)
        }

        linker.addFunction("gfxOpen", DataType.Primitive.INT) { _, values ->
            val (id) = values

            displays[id.toInt()].open()

            Linker.void
        }

        linker.addFunction("gfxClose", DataType.Primitive.INT) { _, values ->
            val (id) = values

            displays[id.toInt()].close()

            Linker.void
        }

        linker.addFunction("gfxIsOpen", DataType.Primitive.INT) { _, values ->
            val (id) = values

            listOf(displays[id.toInt()].isOpen.float)
        }

        linker.addFunction("gfxFlip", DataType.Primitive.INT) { _, values ->
            val (id) = values

            displays[id.toInt()].flip()

            Linker.void
        }

        linker.addFunction(
            "gfxSetColor",
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT
        ) { _, values ->
            val (alpha, blue, green, red, id) = values

            displays[id.toInt()].setColor(red.toInt(), green.toInt(), blue.toInt(), alpha.toInt())

            Linker.void
        }

        linker.addFunction(
            "gfxFillRect",
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT
        ) { _, values ->
            val (height, width, y, x, id) = values

            displays[id.toInt()].fillRect(x.toInt(), y.toInt(), width.toInt(), height.toInt())

            Linker.void
        }
    }

    override fun close(linker: Linker) {
        displays.forEach { it.close() }

        displays.clear()
    }
}