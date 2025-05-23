/*   ______  ____   ____  ____    ____  _____
 * .' ____ \|_  _| |_  _||_   \  /   _||_   _|
 * | (___ \_| \ \   / /    |   \/   |    | |
 *  _.____`.   \ \ / /     | |\  /| |    | |   _
 * | \____) |   \ ' /     _| |_\/_| |_  _| |__/ |
 *  \______.'    \_/     |_____||_____||________|
 *
 *         Stack Virtual Machine Language
 *     Copyright (C) 2024 Christian Alexander
 */
package kakkoiichris.svml.linker.libraries.gfx

import kakkoiichris.svml.lang.parser.DataType
import kakkoiichris.svml.linker.Link
import kakkoiichris.svml.linker.Linker
import kakkoiichris.svml.util.float

object Graphics : Link {
    private val displays = mutableListOf<Display>()

    private var active: Display? = null

    override val name = "graphics"

    override fun open(linker: Linker) {
        linker.addFunction(
            "gfxCreate",
            "IIS",
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.string
        ) { _, data ->
            val width = data.int()
            val height = data.int()
            val title = data.string()

            displays += Display(width, height, title)

            listOf(displays.size.toDouble() - 1)
        }

        linker.addFunction("gfxSetActive", "I", DataType.Primitive.INT) { _, data ->
            val id = data.int()

            active = displays[id]

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
            "IIII",
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT
        ) { _, data ->
            val red = data.int()
            val green = data.int()
            val blue = data.int()
            val alpha = data.int()

            active?.setColor(red, green, blue, alpha)

            Linker.void
        }

        linker.addFunction("gfxClear") { _, _ ->
            active?.clear()

            Linker.void
        }

        linker.addFunction(
            "gfxFillRect",
            "IIII",
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT,
            DataType.Primitive.INT
        ) { _, data ->
            val x = data.int()
            val y = data.int()
            val width = data.int()
            val height = data.int()

            active?.fillRect(x, y, width, height)

            Linker.void
        }
    }

    override fun close(linker: Linker) {
        displays.forEach { it.close() }

        displays.clear()
    }
}