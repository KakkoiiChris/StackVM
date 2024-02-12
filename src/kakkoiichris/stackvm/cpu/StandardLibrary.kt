package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.lang.lexer.Location
import kakkoiichris.stackvm.lang.lexer.TokenType
import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.lang.parser.DataType.Primitive.*
import kakkoiichris.stackvm.lang.parser.Node
import kakkoiichris.stackvm.lang.parser.Signature
import kakkoiichris.stackvm.util.bool
import kakkoiichris.stackvm.util.float
import kakkoiichris.stackvm.util.truncate
import java.io.File
import kotlin.math.*

typealias Method = (values: List<Float>) -> List<Float>

object StandardLibrary {
    private val functionTable = mutableMapOf<String, Int>()

    private val functions = mutableListOf<Function>()

    private val sources = mutableMapOf<String, File>()

    private val void = listOf(0F)

    init {
        val folder = File(javaClass.getResource("/")!!.toURI())

        val files = folder
            .listFiles()!!
            .filter { it.isFile && it.extension == "svml" }

        for (file in files) {
            sources[file.nameWithoutExtension] = file
        }

        addLang()
        addConsole()
        addMath()
    }

    fun hasSource(name: String) =
        name in sources

    fun getSource(name: String) =
        sources[name]!!

    private fun addLang() {
        addFunction("toFloat", INT) { values ->
            val (i) = values

            listOf(i)
        }

        addFunction("toInt", FLOAT) { values ->
            val (i) = values

            listOf(i.toInt().toFloat())
        }

        addFunction("toInt", CHAR) { values ->
            val (i) = values

            listOf(i)
        }

        addFunction("toChar", INT) { values ->
            val (i) = values

            listOf(i)
        }
    }

    private fun addConsole() {
        addFunction("readBool") {
            listOf(
                readln()
                    .toBooleanStrictOrNull()
                    ?.float
                    ?: error("Bool format error!")
            )
        }

        addFunction("readFloat") {
            listOf(
                readln()
                    .toFloatOrNull()
                    ?: error("Number format error!")
            )
        }

        addFunction("readInt") {
            listOf(
                readln()
                    .toIntOrNull()
                    ?.toFloat()
                    ?: error("Number format error!")
            )
        }

        addFunction("readChar") {
            listOf(
                readln()
                    .getOrNull(0)
                    ?.code
                    ?.toFloat()
                    ?: error("Char format error!")
            )
        }

        addFunction("write", BOOL) { values ->
            val (n) = values

            print(n.bool)

            void
        }

        addFunction("write", INT) { values ->
            val (n) = values

            print(n.toInt())

            void
        }

        addFunction("write", FLOAT) { values ->
            val (n) = values

            print(n.truncate())

            void
        }

        addFunction("write", CHAR) { values ->
            val (n) = values

            print(n.toInt().toChar())

            void
        }
    }

    private fun addMath() {
        addFunction("sin", FLOAT) { values ->
            val (n) = values

            listOf(sin(n))
        }

        addFunction("cos", FLOAT) { values ->
            val (n) = values

            listOf(cos(n))
        }

        addFunction("tan", FLOAT) { values ->
            val (n) = values

            listOf(tan(n))
        }

        addFunction("asin", FLOAT) { values ->
            val (n) = values

            listOf(asin(n))
        }

        addFunction("acos", FLOAT) { values ->
            val (n) = values

            listOf(acos(n))
        }

        addFunction("atan", FLOAT) { values ->
            val (n) = values

            listOf(atan(n))
        }

        addFunction("atan2", FLOAT, FLOAT) { values ->
            val (y, x) = values

            listOf(atan2(y, x))
        }

        addFunction("sinh", FLOAT) { values ->
            val (n) = values

            listOf(sinh(n))
        }

        addFunction("cosh", FLOAT) { values ->
            val (n) = values

            listOf(cosh(n))
        }

        addFunction("tanh", FLOAT) { values ->
            val (n) = values

            listOf(tanh(n))
        }

        addFunction("asinh", FLOAT) { values ->
            val (n) = values

            listOf(asinh(n))
        }

        addFunction("acosh", FLOAT) { values ->
            val (n) = values

            listOf(acosh(n))
        }

        addFunction("atanh", FLOAT) { values ->
            val (n) = values

            listOf(atanh(n))
        }

        addFunction("hypot", FLOAT, FLOAT) { values ->
            val (x, y) = values

            listOf(hypot(x, y))
        }

        addFunction("sqrt", FLOAT) { values ->
            val (n) = values

            listOf(sqrt(n))
        }

        addFunction("cbrt", FLOAT) { values ->
            val (n) = values

            listOf(cbrt(n))
        }

        addFunction("exp", FLOAT) { values ->
            val (n) = values

            listOf(exp(n))
        }

        addFunction("expm1", FLOAT) { values ->
            val (n) = values

            listOf(expm1(n))
        }

        addFunction("log", FLOAT, FLOAT) { values ->
            val (n, base) = values

            listOf(log(n, base))
        }

        addFunction("ln", FLOAT) { values ->
            val (n) = values

            listOf(ln(n))
        }

        addFunction("log10", FLOAT) { values ->
            val (n) = values

            listOf(log10(n))
        }

        addFunction("log2", FLOAT) { values ->
            val (n) = values

            listOf(log2(n))
        }

        addFunction("ln1p", FLOAT) { values ->
            val (n) = values

            listOf(ln1p(n))
        }

        addFunction("ceil", FLOAT) { values ->
            val (n) = values

            listOf(ceil(n))
        }

        addFunction("floor", FLOAT) { values ->
            val (n) = values

            listOf(floor(n))
        }

        addFunction("truncate", FLOAT) { values ->
            val (n) = values

            listOf(truncate(n))
        }

        addFunction("round", FLOAT) { values ->
            val (n) = values

            listOf(round(n))
        }

        addFunction("pow", FLOAT, FLOAT) { values ->
            val (b, e) = values

            listOf(b.pow(e))
        }

        addFunction("pow", FLOAT, INT) { values ->
            val (b, e) = values

            listOf(b.pow(e.toInt()))
        }
    }

    private fun addFunction(name: String, vararg params: DataType, method: Method) {
        val node = Node.Name(Location.none(), TokenType.Name(name))

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