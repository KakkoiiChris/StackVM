package kakkoiichris.stackvm.linker.libraries

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker
import kotlin.math.*

object Math : Link {
    override val name = "math"

    override fun open(linker: Linker) {
        Linker.addFunction("sin", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(sin(n))
        }

        Linker.addFunction("cos", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(cos(n))
        }

        Linker.addFunction("tan", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(tan(n))
        }

        Linker.addFunction("asin", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(asin(n))
        }

        Linker.addFunction("acos", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(acos(n))
        }

        Linker.addFunction("atan", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(atan(n))
        }

        Linker.addFunction("atan2", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, values ->
            val (y, x) = values

            listOf(atan2(y, x))
        }

        Linker.addFunction("sinh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(sinh(n))
        }

        Linker.addFunction("cosh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(cosh(n))
        }

        Linker.addFunction("tanh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(tanh(n))
        }

        Linker.addFunction("asinh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(asinh(n))
        }

        Linker.addFunction("acosh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(acosh(n))
        }

        Linker.addFunction("atanh", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(atanh(n))
        }

        Linker.addFunction("hypot", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, values ->
            val (x, y) = values

            listOf(hypot(x, y))
        }

        Linker.addFunction("sqrt", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(sqrt(n))
        }

        Linker.addFunction("cbrt", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(cbrt(n))
        }

        Linker.addFunction("exp", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(exp(n))
        }

        Linker.addFunction("expm1", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(expm1(n))
        }

        Linker.addFunction("log", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, values ->
            val (n, base) = values

            listOf(log(n, base))
        }

        Linker.addFunction("ln", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(ln(n))
        }

        Linker.addFunction("log10", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(log10(n))
        }

        Linker.addFunction("log2", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(log2(n))
        }

        Linker.addFunction("ln1p", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(ln1p(n))
        }

        Linker.addFunction("ceil", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(ceil(n))
        }

        Linker.addFunction("floor", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(floor(n))
        }

        Linker.addFunction("truncate", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(truncate(n))
        }

        Linker.addFunction("round", DataType.Primitive.FLOAT) { _, values ->
            val (n) = values

            listOf(round(n))
        }

        Linker.addFunction("pow", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, values ->
            val (b, e) = values

            listOf(b.pow(e))
        }

        Linker.addFunction("pow", DataType.Primitive.FLOAT, DataType.Primitive.INT) { _, values ->
            val (b, e) = values

            listOf(b.pow(e.toInt()))
        }
    }

    override fun close(linker: Linker) = Unit
}