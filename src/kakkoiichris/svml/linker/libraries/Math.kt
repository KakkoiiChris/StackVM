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
import kotlin.math.*

object Math : Link {
    override val name = "math"

    override fun open(linker: Linker) {
        linker.addFunction("sin", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(sin(n))
        }

        linker.addFunction("cos", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(cos(n))
        }

        linker.addFunction("tan", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(tan(n))
        }

        linker.addFunction("asin", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(asin(n))
        }

        linker.addFunction("acos", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(acos(n))
        }

        linker.addFunction("atan", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(atan(n))
        }

        linker.addFunction("atan2", "FF", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val y=data.nextFloat()
            val x=data.nextFloat()

            listOf(atan2(y, x))
        }

        linker.addFunction("sinh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(sinh(n))
        }

        linker.addFunction("cosh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(cosh(n))
        }

        linker.addFunction("tanh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(tanh(n))
        }

        linker.addFunction("asinh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(asinh(n))
        }

        linker.addFunction("acosh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(acosh(n))
        }

        linker.addFunction("atanh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(atanh(n))
        }

        linker.addFunction("hypot", "FF", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val x=data.nextFloat()
            val y=data.nextFloat()

            listOf(hypot(x, y))
        }

        linker.addFunction("sqrt", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(sqrt(n))
        }

        linker.addFunction("cbrt", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(cbrt(n))
        }

        linker.addFunction("exp", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(exp(n))
        }

        linker.addFunction("expm1", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(expm1(n))
        }

        linker.addFunction("log", "FF", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val n=data.nextFloat()
            val base=data.nextFloat()

            listOf(log(n, base))
        }

        linker.addFunction("ln", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(ln(n))
        }

        linker.addFunction("log10", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(log10(n))
        }

        linker.addFunction("log2", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(log2(n))
        }

        linker.addFunction("ln1p", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(ln1p(n))
        }

        linker.addFunction("ceil", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(ceil(n))
        }

        linker.addFunction("floor", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(floor(n))
        }

        linker.addFunction("truncate", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(truncate(n))
        }

        linker.addFunction("round", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.nextFloat()

            listOf(round(n))
        }

        linker.addFunction("pow", "FF", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val b=data.nextFloat()
            val e=data.nextFloat()

            listOf(b.pow(e))
        }

        linker.addFunction("pow", "FI", DataType.Primitive.FLOAT, DataType.Primitive.INT) { _, data ->
            val b=data.nextFloat()
            val e=data.nextFloat()

            listOf(b.pow(e.toInt()))
        }
    }

    override fun close(linker: Linker) = Unit
}