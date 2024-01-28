package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.lang.*
import kakkoiichris.stackvm.lang.DataType.Primitive.*
import kakkoiichris.stackvm.util.toBool
import kakkoiichris.stackvm.util.toFloat
import kakkoiichris.stackvm.util.truncate
import kotlin.math.*

typealias Method = (values: List<Float>) -> Float

object SystemFunctions {
    private val functionTable = mutableMapOf<String, Int>()

    private val functions = mutableListOf<Function>()

    init {
        addMath()

        addConsole()
    }

    private fun addMath() {
        addFunction("sin", FLOAT) { values ->
            val (n) = values

            sin(n)
        }

        addFunction("cos", FLOAT) { values ->
            val (n) = values

            cos(n)
        }

        addFunction("tan", FLOAT) { values ->
            val (n) = values

            tan(n)
        }

        addFunction("asin", FLOAT) { values ->
            val (n) = values

            asin(n)
        }

        addFunction("acos", FLOAT) { values ->
            val (n) = values

            acos(n)
        }

        addFunction("atan", FLOAT) { values ->
            val (n) = values

            atan(n)
        }

        addFunction("atan2", FLOAT, FLOAT) { values ->
            val (y, x) = values

            atan2(y, x)
        }

        addFunction("sinh", FLOAT) { values ->
            val (n) = values

            sinh(n)
        }

        addFunction("cosh", FLOAT) { values ->
            val (n) = values

            cosh(n)
        }

        addFunction("tanh", FLOAT) { values ->
            val (n) = values

            tanh(n)
        }

        addFunction("asinh", FLOAT) { values ->
            val (n) = values

            asinh(n)
        }

        addFunction("acosh", FLOAT) { values ->
            val (n) = values

            acosh(n)
        }

        addFunction("atanh", FLOAT) { values ->
            val (n) = values

            atanh(n)
        }

        addFunction("hypot", FLOAT, FLOAT) { values ->
            val (x, y) = values

            hypot(x, y)
        }

        addFunction("sqrt", FLOAT) { values ->
            val (n) = values

            sqrt(n)
        }

        addFunction("cbrt", FLOAT) { values ->
            val (n) = values

            cbrt(n)
        }

        addFunction("exp", FLOAT) { values ->
            val (n) = values

            exp(n)
        }

        addFunction("expm1", FLOAT) { values ->
            val (n) = values

            expm1(n)
        }

        addFunction("log", FLOAT, FLOAT) { values ->
            val (n, base) = values

            log(n, base)
        }

        addFunction("ln", FLOAT) { values ->
            val (n) = values

            ln(n)
        }

        addFunction("log10", FLOAT) { values ->
            val (n) = values

            log10(n)
        }

        addFunction("log2", FLOAT) { values ->
            val (n) = values

            log2(n)
        }

        addFunction("ln1p", FLOAT) { values ->
            val (n) = values

            ln1p(n)
        }

        addFunction("ceil", FLOAT) { values ->
            val (n) = values

            ceil(n)
        }

        addFunction("floor", FLOAT) { values ->
            val (n) = values

            floor(n)
        }

        addFunction("truncate", FLOAT) { values ->
            val (n) = values

            truncate(n)
        }

        addFunction("round", FLOAT) { values ->
            val (n) = values

            round(n)
        }

        addFunction("pow", FLOAT, FLOAT) { values ->
            val (b, e) = values

            b.pow(e)
        }

        addFunction("pow", FLOAT, INT) { values ->
            val (b, e) = values
            Float.NEGATIVE_INFINITY
            b.pow(e.toInt())
        }
    }

    private fun addConsole() {
        addFunction("readBool") {
            readln()
                .toBooleanStrictOrNull()
                ?.toFloat()
                ?: error("Bool format error!")
        }

        addFunction("readFloat") {
            readln()
                .toFloatOrNull()
                ?: error("Number format error!")
        }

        addFunction("readInt") {
            readln()
                .toIntOrNull()
                ?.toFloat()
                ?: error("Number format error!")
        }

        addFunction("readChar") {
            readln()
                .getOrNull(0)
                ?.code
                ?.toFloat()
                ?: error("Char format error!")
        }

        addFunction("write", BOOL) { values ->
            val (n) = values

            print(n.toBool())

            0F
        }

        addFunction("write", INT) { values ->
            val (n) = values

            print(n.toInt())

            0F
        }

        addFunction("write", FLOAT) { values ->
            val (n) = values

            print(n.truncate())

            0F
        }

        addFunction("write", CHAR) { values ->
            val (n) = values

            print(n.toInt().toChar())

            0F
        }
    }

    private fun addFunction(name: String, vararg params: DataType, method: Method) {
        val node = Node.Name(Location.none, TokenType.Name(name))

        val signature = Signature(node, params.toList())

        val function = Function(signature, method)

        functions += function

        functionTable[signature.toString()] = functions.size - 1
    }

    operator fun get(signature: Signature) =
        functionTable[signature.toString()] ?: -1

    operator fun get(id: Int) =
        functions[id]

    class Function(val signature: Signature, private val method: Method) {
        operator fun invoke(values: List<Float>) =
            method.invoke(values)
    }
}