package kakkoiichris.stackvm.linker.libraries

import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker
import kotlin.math.*

object Math : Link {
    override val name = "math"

    override fun open(linker: Linker) {
        linker.addFunction("sin", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(sin(n))
        }

        linker.addFunction("cos", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(cos(n))
        }

        linker.addFunction("tan", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(tan(n))
        }

        linker.addFunction("asin", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(asin(n))
        }

        linker.addFunction("acos", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(acos(n))
        }

        linker.addFunction("atan", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(atan(n))
        }

        linker.addFunction("atan2", "FF", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val y=data.float(0)
            val x=data.float(1)

            listOf(atan2(y, x))
        }

        linker.addFunction("sinh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(sinh(n))
        }

        linker.addFunction("cosh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(cosh(n))
        }

        linker.addFunction("tanh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(tanh(n))
        }

        linker.addFunction("asinh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(asinh(n))
        }

        linker.addFunction("acosh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(acosh(n))
        }

        linker.addFunction("atanh", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(atanh(n))
        }

        linker.addFunction("hypot", "FF", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val x=data.float(0)
            val y=data.float(1)

            listOf(hypot(x, y))
        }

        linker.addFunction("sqrt", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(sqrt(n))
        }

        linker.addFunction("cbrt", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(cbrt(n))
        }

        linker.addFunction("exp", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(exp(n))
        }

        linker.addFunction("expm1", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(expm1(n))
        }

        linker.addFunction("log", "FF", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val n=data.float(0)
            val base=data.float(1)

            listOf(log(n, base))
        }

        linker.addFunction("ln", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(ln(n))
        }

        linker.addFunction("log10", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(log10(n))
        }

        linker.addFunction("log2", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(log2(n))
        }

        linker.addFunction("ln1p", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(ln1p(n))
        }

        linker.addFunction("ceil", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(ceil(n))
        }

        linker.addFunction("floor", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(floor(n))
        }

        linker.addFunction("truncate", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(truncate(n))
        }

        linker.addFunction("round", "F", DataType.Primitive.FLOAT) { _, data ->
            val n = data.float(0)

            listOf(round(n))
        }

        linker.addFunction("pow", "FF", DataType.Primitive.FLOAT, DataType.Primitive.FLOAT) { _, data ->
            val b=data.float(0)
            val e=data.float(1)

            listOf(b.pow(e))
        }

        linker.addFunction("pow", "FI", DataType.Primitive.FLOAT, DataType.Primitive.INT) { _, data ->
            val b=data.float(0)
            val e=data.float(1)

            listOf(b.pow(e.toInt()))
        }
    }

    override fun close(linker: Linker) = Unit
}