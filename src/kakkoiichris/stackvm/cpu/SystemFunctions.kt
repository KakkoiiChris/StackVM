package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.lang.Node
import kakkoiichris.stackvm.util.truncate
import kotlin.math.abs

typealias Method = (values: List<Float>) -> Float

object SystemFunctions {
    private val functionTable = mutableMapOf<String, Int>()

    private val functions = mutableListOf<Function>()

    init {
        addFunction("abs", Function(1) { values ->
            val (n) = values

            abs(n)
        })

        addFunction("read", Function {
            readln().toFloatOrNull() ?: error("Number format error!")
        })

        addFunction("write", Function(1) { values ->
            val (n) = values

            println(n.truncate())

            0F
        })
    }

    private fun addFunction(name: String, function: Function) {
        functions += function

        functionTable[name] = functions.size - 1
    }

    operator fun get(name: Node.Name) =
        functionTable[name.name.value] ?: -1

    operator fun get(id: Int) =
        functions[id]

    class Function(val arity: Int = 0, private val method: Method) {
        operator fun invoke(values: List<Float>) =
            method.invoke(values)
    }
}