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

    fun addVariable(isConstant: Boolean, name: TokenType.Name, dataType: DataType, location: Location) {
        val scope = peek()

        if (scope.addVariable(isConstant, name, dataType)) return

        error("Redeclared variable '${name.value}' @ ${location}!")
    }

    fun getVariable(variable: Node.Variable): Lookup =
        getVariable(variable.name, variable.location)

    fun getVariable(name: TokenType.Name, location: Location): Lookup {
        var here: Scope? = peek()

        while (here != null && here != global) {
            val variable = here.getVariable(name)

            if (variable != null) return Lookup(false, variable)

            here = here.parent
        }

        val variable = global.getVariable(name)

        if (variable != null) return Lookup(true, variable)

        error("Undeclared variable '${name.value}' @ ${location}!")
    }

    fun getFunctionID() = functionID++

    fun addFunction(dataType: DataType, id: Int, signature: Signature, isNative: Boolean) {
        if (peek().addFunction(dataType, id, signature, isNative)) return

        if (global.addFunction(dataType, id, signature, isNative)) return

        error("Redeclared function '$signature' @ ${signature.name.location}!")
    }

    fun getFunction(signature: Signature): FunctionRecord {
        var here: Scope? = peek()

        while (here != null) {
            val record = here.getFunction(signature)

            if (record != null) return record

            here = here.parent
        }

        error("Undeclared function '$signature' @ ${signature.name.location}!")
    }

    class Scope(val parent: Scope? = null) {
        var variableID: Int = parent?.variableID ?: 0

        private val variables = mutableMapOf<String, VariableRecord>()
        private val functions = mutableMapOf<String, FunctionRecord>()

        fun addVariable(isConstant: Boolean, name: TokenType.Name, dataType: DataType): Boolean {
            if (name.value in variables) return false

            variables[name.value] = VariableRecord(isConstant, dataType, variableID)

            variableID++

            return true
        }

        fun getVariable(name: TokenType.Name) =
            variables[name.value]

        fun addFunction(dataType: DataType, id: Int, signature: Signature, isNative: Boolean): Boolean {
            val rep = signature.toString()

            if (rep in functions) return false

            functions[rep] = FunctionRecord(isNative, dataType, id)

            return true
        }

        fun getFunction(signature: Signature) =
            functions[signature.toString()]
    }

    data class Lookup(val isGlobal: Boolean, val record: VariableRecord)

    data class VariableRecord(val isConstant: Boolean, val dataType: DataType, val id: Int)

    data class FunctionRecord(val isNative: Boolean, val dataType: DataType, val id: Int)
}