package kakkoiichris.stackvm.lang.parser

import kakkoiichris.stackvm.lang.lexer.Context
import kakkoiichris.stackvm.lang.lexer.TokenType
import java.util.*

object Memory {
    private val scopes = Stack<Scope>()

    private val global = Scope()

    private var variableID = 0
    private var functionID = 0

    init {
        scopes.push(global)
    }

    fun reset() {
        scopes.clear()

        global.clear()
    }

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

    private fun peek(): Scope {
        return scopes.peek()
    }

    fun addVariable(
        isConstant: Boolean,
        isMutable: Boolean,
        name: TokenType.Name,
        dataType: DataType,
        context: Context
    ) {
        val scope = peek()

        if (scope.addVariable(isConstant, isMutable, name, dataType)) return

        error("Redeclared variable '${name.value}' @ ${context}!")
    }

    fun getVariable(variable: Node.Variable): Lookup =
        getVariable(variable.name, variable.context)

    fun getVariable(name: TokenType.Name, context: Context): Lookup {
        var here: Scope? = peek()

        while (here != null && here != global) {
            val variable = here.getVariable(name)

            if (variable != null) return Lookup(false, variable)

            here = here.parent
        }

        val variable = global.getVariable(name)

        if (variable != null) return Lookup(true, variable)

        error("Undeclared variable '${name.value}' @ ${context}!")
    }

    fun getFunctionID() = functionID++

    fun addFunction(dataType: DataType, signature: Signature, isNative: Boolean): Boolean {
        if (peek().addFunction(dataType, signature, isNative)) return true

        if (global.addFunction(dataType, signature, isNative)) return true

        return false
    }

    fun getFunction(signature: Signature): FunctionRecord {
        var here: Scope? = peek()

        while (here != null) {
            val record = here.getFunction(signature)

            if (record != null) return record

            here = here.parent
        }

        error("Undeclared function '$signature' @ ${signature.name.context}!")
    }

    class Scope(val parent: Scope? = null) {
        private val variables = mutableMapOf<String, VariableRecord>()
        private val functions = mutableMapOf<String, FunctionRecord>()

        fun clear() {
            variables.clear()
            functions.clear()
        }

        fun addVariable(isConstant: Boolean, isMutable: Boolean, name: TokenType.Name, dataType: DataType): Boolean {
            if (name.value in variables) return false

            variables[name.value] = VariableRecord(isConstant, isMutable, dataType, variableID++)

            return true
        }

        fun getVariable(name: TokenType.Name) =
            variables[name.value]

        fun addFunction(dataType: DataType, signature: Signature, isNative: Boolean): Boolean {
            val rep = signature.toString()

            if (rep in functions) return false

            functions[rep] = FunctionRecord(isNative, dataType, signature.hashCode())

            return true
        }

        fun getFunction(signature: Signature) =
            functions[signature.toString()]
    }

    data class Lookup(val isGlobal: Boolean, val record: VariableRecord)

    data class VariableRecord(val isConstant: Boolean, val isMutable: Boolean, val dataType: DataType, val id: Int)

    data class FunctionRecord(val isNative: Boolean, val dataType: DataType, val id: Int)
}