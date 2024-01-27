package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.lang.*
import kakkoiichris.stackvm.lang.DataType.Primitive.*
import kakkoiichris.stackvm.util.toBool
import kakkoiichris.stackvm.util.truncate
import kotlin.math.abs

typealias Method = (values: List<Float>) -> Float

object SystemFunctions {
    private val functionTable = mutableMapOf<String, Int>()

    private val functions = mutableListOf<Function>()

    init {
        addFunction("abs", FLOAT) { values ->
            val (n) = values

            abs(n)
        }

        addFunction("read") {
            readln().toFloatOrNull() ?: error("Number format error!")
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