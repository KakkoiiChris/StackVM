package kakkoiichris.stackvm.lang

import java.util.*

class Memory {
    private val scopes = Stack<Scope>()

    private val global = Scope()

    private var functionID = 0

    fun open() = push(global)

    fun close() = pop()

    fun push(scope: Scope? = null) {
        if (scope != null) {
            scopes.push(scope)
        }
        else if (scopes.isNotEmpty()) {
            val parent = peek()

            val next = Scope(parent)

            scopes.push(next)
        }
        else {
            scopes.push(Scope())
        }
    }

    fun pop(): Scope? {
        if (scopes.isNotEmpty()) {
            return scopes.pop()
        }

        return null
    }

    fun peek(): Scope {
        return scopes.peek()
    }

    fun addVariable(constant: Boolean, name: TokenType.Name, dataType: DataType, location: Location) {
        val scope = peek()

        if (scope.addVariable(constant, name, dataType)) return

        error("Redeclared variable '${name.value}' @ ${location}!")
    }

    fun getVariable(variable: Node.Variable): Lookup =
        getVariable(variable.name, variable.location)

    fun getVariable(name: TokenType.Name, location: Location): Lookup {
        var here: Scope? = peek()

        while (here != null && here != global) {
            val variable = here.getVariable(name)

            if (variable != null) return Lookup(Lookup.Mode.LOCAL, variable)

            here = here.parent
        }

        val variable = global.getVariable(name)

        if (variable != null) return Lookup(Lookup.Mode.GLOBAL, variable)

        error("Undeclared variable '${name.value}' @ ${location}!")
    }

    fun getFunctionID() = functionID++

    fun addFunction(dataType: DataType, id: Int, signature: Signature) {
        if (peek().addFunction(dataType, id, signature)) return

        if (global.addFunction(dataType, id, signature)) return

        error("Redeclared function '$signature' @ ${signature.name.location}!")
    }

    fun getFunction(signature: Signature): FunctionActivation {
        var here: Scope? = peek()

        while (here != null) {
            val activation = here.getFunction(signature)

            if (activation != null) return activation

            here = here.parent
        }

        error("Undeclared function '$signature' @ ${signature.name.location}!")
    }

    class Scope(val parent: Scope? = null) {
        var variableID: Int = parent?.variableID ?: 0

        private val variables = mutableMapOf<String, VariableActivation>()
        private val functions = mutableMapOf<String, FunctionActivation>()

        fun addVariable(constant: Boolean, name: TokenType.Name, dataType: DataType): Boolean {
            if (name.value in variables) return false

            variables[name.value] = VariableActivation(constant, dataType, variableID++)

            return true
        }

        fun getVariable(name: TokenType.Name) =
            variables[name.value]

        fun addFunction(dataType: DataType, id: Int, signature: Signature): Boolean {
            val rep = signature.toString()

            if (rep in functions) return false

            functions[rep] = FunctionActivation(dataType, id)

            return true
        }

        fun getFunction(signature: Signature) =
            functions[signature.toString()]
    }

    data class Lookup(val mode: Mode, val activation: VariableActivation) {
        enum class Mode {
            GLOBAL,
            LOCAL
        }
    }

    data class VariableActivation(val constant: Boolean, val dataType: DataType, val address: Int)

    data class FunctionActivation(val dataType: DataType, val address: Int)
}