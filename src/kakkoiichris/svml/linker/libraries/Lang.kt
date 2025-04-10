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
package kakkoiichris.svml.linker.libraries

import kakkoiichris.svml.lang.parser.DataType
import kakkoiichris.svml.linker.Link
import kakkoiichris.svml.linker.Linker

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