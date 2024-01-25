package kakkoiichris.stackvm.lang

import java.util.*

class Memory {
    private val scopes = Stack<Scope>()

    private val global = Scope()

    private var functionID = 0

    fun open() = push(global)

    fun close() = pop()

    fun push(scope: Scope = Scope()) {
        if (scopes.isNotEmpty()) {
            val parent = peek()

            val next = Scope(parent)

            scopes.push(next)
        }
        else {
            scopes.push(scope)
        }
    }

    fun pop() {
        if (scopes.isNotEmpty()) {
            scopes.pop()
        }
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

    fun addFunction(name: Node.Name): Int {
        val id = functionID++

        if (peek().addFunction(name, id)) return id

        if (global.addFunction(name, id)) return id

        error("Redeclared function '${name.name.value}' @ ${name.location}!")
    }

    fun getFunction(name: Node.Name): Int {
        var here: Scope? = peek()

        while (here != null) {
            val function = here.getFunction(name)

            if (function != null) return function

            here = here.parent
        }

        error("Undeclared function '${name.name.value}' @ ${name.location}!")
    }

    class Scope(val parent: Scope? = null) {
        var variableID: Int = parent?.variableID ?: 0

        private val variables = mutableMapOf<String, Activation>()
        private val functions = mutableMapOf<String, Int>()

        fun addVariable(constant: Boolean, name: TokenType.Name, dataType: DataType): Boolean {
            if (name.value in variables) return false

            variables[name.value] = Activation(constant, dataType, variableID++)

            return true
        }

        fun getVariable(name: TokenType.Name) =
            variables[name.value]

        fun addFunction(name: Node.Name, id: Int): Boolean {
            if (name.name.value in functions) return false

            functions[name.name.value] = id

            return true
        }

        fun getFunction(name: Node.Name) =
            functions[name.name.value]
    }

    data class Lookup(val mode: Mode, val activation: Activation) {
        enum class Mode {
            GLOBAL,
            LOCAL
        }
    }

    data class Activation(val constant: Boolean, val dataType: DataType, val address: Int)
}