package kakkoiichris.stackvm.linker.libraries

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker
import kotlin.math.*

object Math : Link {
    override val name = "math"

    override fun open(linker: Linker) {
        linker.addFunction("sin", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(sin(n))
        }

        linker.addFunction("cos", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(cos(n))
        }

        linker.addFunction("tan", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(tan(n))
        }

        linker.addFunction("asin", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(asin(n))
        }

        linker.addFunction("acos", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(acos(n))
        }

        linker.addFunction("atan", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(atan(n))
        }

        linker.addFunction("atan2", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, values ->
            val (y, x) = values

            listOf(atan2(y, x))
        }

        linker.addFunction("sinh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(sinh(n))
        }

        linker.addFunction("cosh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(cosh(n))
        }

        linker.addFunction("tanh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(tanh(n))
        }

        linker.addFunction("asinh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(asinh(n))
        }

        linker.addFunction("acosh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(acosh(n))
        }

        linker.addFunction("atanh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(atanh(n))
        }

        linker.addFunction("hypot", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, values ->
            val (x, y) = values

            listOf(hypot(x, y))
        }

        linker.addFunction("sqrt", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(sqrt(n))
        }

        linker.addFunction("cbrt", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(cbrt(n))
        }

        linker.addFunction("exp", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(exp(n))
        }

        linker.addFunction("expm1", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(expm1(n))
        }

        linker.addFunction("log", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, values ->
            val (n, base) = values

            listOf(log(n, base))
        }

        linker.addFunction("ln", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(ln(n))
        }

        linker.addFunction("log10", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(log10(n))
        }

        linker.addFunction("log2", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(log2(n))
        }

        linker.addFunction("ln1p", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(ln1p(n))
        }

        linker.addFunction("ceil", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(ceil(n))
        }

        linker.addFunction("floor", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(floor(n))
        }

        linker.addFunction("truncate", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(truncate(n))
        }

        linker.addFunction("round", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(round(n))
        }

        linker.addFunction("pow", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, values ->
            val (b, e) = values

            listOf(b.pow(e))
        }

        linker.addFunction("pow", DataType.Primitive.FLOAT, DataType.Primitive.INT) { _, values ->
            val (b, e) = values

            listOf(b.pow(e.toInt()))
        }
    }

    override fun close(linker: Linker) = Unit
}