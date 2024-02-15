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

typealias Method = (cpu: CPU, values: List<Float>) -> List<Float>

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

    fun hasFile(name: String) =
        name in sources

    fun getFile(name: String) =
        sources[name]!!

    fun hasFunction(signature: Signature): Boolean =
        signature.toString() in functionTable

    operator fun get(signature: Signature) =
        functionTable[signature.toString()]!!

    operator fun get(id: Int) =
        functions[id]

    private fun addLang() {
        addFunction("toFloat", INT) { _, values ->
            val (i) = values

            listOf(i)
        }

        addFunction("toInt", FLOAT) { _, values ->
            val (i) = values

            listOf(i.toInt().toFloat())
        }

        addFunction("toInt", CHAR) { _, values ->
            val (i) = values

            listOf(i)
        }

        addFunction("toChar", INT) { _, values ->
            val (i) = values

            listOf(i)
        }

        addFunction("exit", INT) { cpu, values ->
            val (code) = values

            cpu.result = code
            cpu.running = false

            println("Exited application with code '${code.truncate()}'.")

            void
        }
    }

    private fun addConsole() {
        addFunction("readBool") { _, _ ->
            listOf(
                readln()
                    .toBooleanStrictOrNull()
                    ?.float
                    ?: error("Bool format error!")
            )
        }

        addFunction("readFloat") { _, _ ->
            listOf(
                readln()
                    .toFloatOrNull()
                    ?: error("Number format error!")
            )
        }

        addFunction("readInt") { _, _ ->
            listOf(
                readln()
                    .toIntOrNull()
                    ?.toFloat()
                    ?: error("Number format error!")
            )
        }

        addFunction("readChar") { _, _ ->
            listOf(
                readln()
                    .getOrNull(0)
                    ?.code
                    ?.toFloat()
                    ?: error("Char format error!")
            )
        }

        addFunction("write", BOOL) { _, values ->
            val (n) = values

            print(n.bool)

            void
        }

        addFunction("write", INT) { _, values ->
            val (n) = values

            print(n.toInt())

            void
        }

        addFunction("write", FLOAT) { _, values ->
            val (n) = values

            print(n.truncate())

            void
        }

        addFunction("write", CHAR) { _, values ->
            val (n) = values

            print(n.toInt().toChar())

            void
        }
    }

    private fun addMath() {
        addFunction("sin", FLOAT) { _, values ->
            val (n) = values

            listOf(sin(n))
        }

        addFunction("cos", FLOAT) { _, values ->
            val (n) = values

            listOf(cos(n))
        }

        addFunction("tan", FLOAT) { _, values ->
            val (n) = values

            listOf(tan(n))
        }

        addFunction("asin", FLOAT) { _, values ->
            val (n) = values

            listOf(asin(n))
        }

        addFunction("acos", FLOAT) { _, values ->
            val (n) = values

            listOf(acos(n))
        }

        addFunction("atan", FLOAT) { _, values ->
            val (n) = values

            listOf(atan(n))
        }

        addFunction("atan2", FLOAT, FLOAT) { _, values ->
            val (y, x) = values

            listOf(atan2(y, x))
        }

        addFunction("sinh", FLOAT) { _, values ->
            val (n) = values

            listOf(sinh(n))
        }

        addFunction("cosh", FLOAT) { _, values ->
            val (n) = values

            listOf(cosh(n))
        }

        addFunction("tanh", FLOAT) { _, values ->
            val (n) = values

            listOf(tanh(n))
        }

        addFunction("asinh", FLOAT) { _, values ->
            val (n) = values

            listOf(asinh(n))
        }

        addFunction("acosh", FLOAT) { _, values ->
            val (n) = values

            listOf(acosh(n))
        }

        addFunction("atanh", FLOAT) { _, values ->
            val (n) = values

            listOf(atanh(n))
        }

        addFunction("hypot", FLOAT, FLOAT) { _, values ->
            val (x, y) = values

            listOf(hypot(x, y))
        }

        addFunction("sqrt", FLOAT) { _, values ->
            val (n) = values

            listOf(sqrt(n))
        }

        addFunction("cbrt", FLOAT) { _, values ->
            val (n) = values

            listOf(cbrt(n))
        }

        addFunction("exp", FLOAT) { _, values ->
            val (n) = values

            listOf(exp(n))
        }

        addFunction("expm1", FLOAT) { _, values ->
            val (n) = values

            listOf(expm1(n))
        }

        addFunction("log", FLOAT, FLOAT) { _, values ->
            val (n, base) = values

            listOf(log(n, base))
        }

        addFunction("ln", FLOAT) { _, values ->
            val (n) = values

            listOf(ln(n))
        }

        addFunction("log10", FLOAT) { _, values ->
            val (n) = values

            listOf(log10(n))
        }

        addFunction("log2", FLOAT) { _, values ->
            val (n) = values

            listOf(log2(n))
        }

        addFunction("ln1p", FLOAT) { _, values ->
            val (n) = values

            listOf(ln1p(n))
        }

        addFunction("ceil", FLOAT) { _, values ->
            val (n) = values

            listOf(ceil(n))
        }

        addFunction("floor", FLOAT) { _, values ->
            val (n) = values

            listOf(floor(n))
        }

        addFunction("truncate", FLOAT) { _, values ->
            val (n) = values

            listOf(truncate(n))
        }

        addFunction("round", FLOAT) { _, values ->
            val (n) = values

            listOf(round(n))
        }

        addFunction("pow", FLOAT, FLOAT) { _, values ->
            val (b, e) = values

            listOf(b.pow(e))
        }

        addFunction("pow", FLOAT, INT) { _, values ->
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

    class Function(val signature: Signature, private val method: Method) {
        operator fun invoke(cpu: CPU, values: List<Float>) =
            method.invoke(cpu, values)
    }
}