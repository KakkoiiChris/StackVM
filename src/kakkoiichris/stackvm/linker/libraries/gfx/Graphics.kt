package kakkoiichris.stackvm.linker.libraries.gfx

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker
import kakkoiichris.stackvm.util.float

object Graphics : Link {
    private val displays = mutableListOf<Display>()

    private var active: Display? = null

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

        linker.addFunction("gfxSetActive", DataType.Primitive.INT) { _, values ->
            val (id) = values

            active = displays[id.toInt()]

            Linker.void
        }

        linker.addFunction("gfxOpen") { _, _ ->
            active?.open()

            Linker.void
        }

        linker.addFunction("gfxClose") { _, _ ->
            active?.close()

            Linker.void
        }

        linker.addFunction("gfxIsOpen") { _, _ ->
            listOf(active?.isOpen?.float ?: 0.0)
        }

        linker.addFunction("gfxFlip") { _, _ ->
            active?.flip()

            Linker.void
        }

        linker.addFunction(
            "gfxSetColor",
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT
        ) { _, values ->
            val (alpha, blue, green, red) = values

            active?.setColor(red.toInt(), green.toInt(), blue.toInt(), alpha.toInt())

            Linker.void
        }

        linker.addFunction("gfxClear") { _, _ ->
            active?.clear()

            Linker.void
        }

        linker.addFunction(
            "gfxFillRect",
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT
        ) { _, values ->
            val (height, width, y, x) = values

            active?.fillRect(x.toInt(), y.toInt(), width.toInt(), height.toInt())

            Linker.void
        }
    }

    override fun close(linker: Linker) {
        displays.forEach { it.close() }

        displays.clear()
    }
}