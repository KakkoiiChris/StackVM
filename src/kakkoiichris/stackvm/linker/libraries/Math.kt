package kakkoiichris.stackvm.linker.libraries

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker
import kotlin.math.*

object Math : Link {
    override val name = "math"

    override fun open(linker: Linker) {
        linker.addFunction("sin", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(sin(n))
        }

        linker.addFunction("cos", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(cos(n))
        }

        linker.addFunction("tan", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(tan(n))
        }

        linker.addFunction("asin", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(asin(n))
        }

        linker.addFunction("acos", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(acos(n))
        }

        linker.addFunction("atan", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(atan(n))
        }

        linker.addFunction("atan2", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val y = data.float()
            val x = data.float()

            listOf(atan2(y, x))
        }

        linker.addFunction("sinh", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(sinh(n))
        }

        linker.addFunction("cosh", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(cosh(n))
        }

        linker.addFunction("tanh", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(tanh(n))
        }

        linker.addFunction("asinh", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(asinh(n))
        }

        linker.addFunction("acosh", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(acosh(n))
        }

        linker.addFunction("atanh", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(atanh(n))
        }

        linker.addFunction("hypot", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val x = data.float()
            val y = data.float()

            listOf(hypot(x, y))
        }

        linker.addFunction("sqrt", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(sqrt(n))
        }

        linker.addFunction("cbrt", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(cbrt(n))
        }

        linker.addFunction("exp", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(exp(n))
        }

        linker.addFunction("expm1", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(expm1(n))
        }

        linker.addFunction("log", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()
            val base = data.float()

            listOf(log(n, base))
        }

        linker.addFunction("ln", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(ln(n))
        }

        linker.addFunction("log10", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(log10(n))
        }

        linker.addFunction("log2", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(log2(n))
        }

        linker.addFunction("ln1p", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(ln1p(n))
        }

        linker.addFunction("ceil", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(ceil(n))
        }

        linker.addFunction("floor", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(floor(n))
        }

        linker.addFunction("truncate", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(truncate(n))
        }

        linker.addFunction("round", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float()

            listOf(round(n))
        }

        linker.addFunction("pow", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val b = data.float()
            val e = data.float()

            listOf(b.pow(e))
        }

        linker.addFunction("pow", DataType.Primitive.FLOAT, DataType.Primitive.INT) { _, data ->
            val b = data.float()
            val e = data.float()

            listOf(b.pow(e.toInt()))
        }
    }

    override fun close(linker: Linker) = Unit
}