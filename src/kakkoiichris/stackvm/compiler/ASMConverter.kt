package kakkoiichris.stackvm.compiler

import kakkoiichris.stackvm.asm.ASMToken
import kakkoiichris.stackvm.asm.ASMToken.Instruction.*
import kakkoiichris.stackvm.lang.Node
import kakkoiichris.stackvm.lang.Parser
import kakkoiichris.stackvm.lang.TokenType.Symbol.*
import java.util.*

class ASMConverter(private val parser: Parser, private val optimize: Boolean) : Node.Visitor<List<IASMToken>> {
    private var pos = 0

    private val memory = Memory()


    fun convert(): List<ASMToken> {
        try {
            memory.open()

            val tokens = mutableListOf<ASMToken>()

            for (statement in parser) {
                val iTokens = visit(statement)

                val subTokens = iTokens.filterIsInstance<IASMToken.Ok>()

                if (iTokens.size > subTokens.size) error("Unresolved intermediate token.")

                tokens.addAll(subTokens.map { it.token })
            }

            tokens.add(HALT)

            if (optimize) {
                var i = 0

                while (i < tokens.lastIndex) {
                    val a = tokens[i]
                    val b = tokens[i + 1]

                    if (a === b && a in listOf(NOT, NEG)) {
                        repeat(2) { tokens.removeAt(i) }
                    }
                    else {
                        i++
                    }
                }
            }

            return tokens
        }
        finally {
            memory.close()
        }
    }

    private fun resolveStartAndEnd(iTokens: List<IASMToken>, start: Float, end: Float) =
        iTokens
            .map { it.resolveStartAndEnd(start, end) ?: it }
            .toMutableList()

    private fun resolveLast(iTokens: List<IASMToken>, last: Float) =
        iTokens
            .map { it.resolveLast(last) ?: it }
            .toMutableList()

    private fun load(mode: Memory.Lookup.Mode) = when (mode) {
        Memory.Lookup.Mode.GLOBAL -> LOADG.iasm
        Memory.Lookup.Mode.LOCAL  -> LOAD.iasm
    }

    override fun visitVar(node: Node.Var): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.node)

        memory.addVariable(node.name)

        val (_, address) = memory
            .getVariable(node.name)

        iTokens += STORE.iasm
        iTokens += ASMToken.Value(address.toFloat()).iasm

        pos += 2

        return iTokens
    }

    override fun visitIf(node: Node.If): List<IASMToken> {
        var iTokens = mutableListOf<IASMToken>()

        var last = -1F

        for ((i, branch) in node.branches.withIndex()) {
            val (_, condition, body) = branch

            try {
                memory.push()

                val start = pos.toFloat()

                if (condition != null) {
                    iTokens += visit(condition)

                    iTokens += NOT.iasm
                    iTokens += JIF.iasm
                    iTokens += IASMToken.AwaitEnd()

                    pos += 3
                }

                for (stmt in body) {
                    iTokens += visit(stmt)
                }

                if (i != node.branches.lastIndex) {
                    iTokens += JMP.iasm
                    iTokens += IASMToken.AwaitLast()

                    pos += 2
                }

                val end = pos.toFloat()
                last = end

                iTokens = resolveStartAndEnd(iTokens, start, end)
            }
            finally {
                memory.pop()
            }
        }

        iTokens = resolveLast(iTokens, last)

        return iTokens
    }

    override fun visitWhile(node: Node.While): List<IASMToken> {
        var iTokens = mutableListOf<IASMToken>()

        try {
            memory.push()

            val start = pos.toFloat()

            iTokens += visit(node.condition)

            iTokens += NOT.iasm
            iTokens += JIF.iasm
            iTokens += IASMToken.AwaitEnd()

            pos += 3

            for (stmt in node.body) {
                iTokens += visit(stmt)
            }

            iTokens += JMP.iasm
            iTokens += IASMToken.AwaitStart()

            pos += 2

            val end = pos.toFloat()

            iTokens = resolveStartAndEnd(iTokens, start, end)
        }
        finally {
            memory.pop()
        }

        return iTokens
    }

    override fun visitBreak(node: Node.Break): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += JMP.iasm
        iTokens += IASMToken.AwaitEnd()

        pos += 2

        return iTokens
    }

    override fun visitContinue(node: Node.Continue): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += JMP.iasm
        iTokens += IASMToken.AwaitStart()

        pos += 2

        return iTokens
    }

    override fun visitFunction(node: Node.Function): List<IASMToken> {
        var iTokens = mutableListOf<IASMToken>()

        try {
            iTokens += JMP.iasm
            iTokens += IASMToken.AwaitEnd()

            pos += 2

            memory.addFunction(node.name, pos)

            memory.push()

            val start = pos.toFloat()

            for (param in node.params) {
                memory.addVariable(param)

                val (_, address) = memory
                    .getVariable(param)

                iTokens += STORE.iasm
                iTokens += ASMToken.Value(address.toFloat()).iasm

                pos += 2
            }

            for (stmt in node.body) {
                iTokens += visit(stmt)
            }

            if (iTokens.none { it is IASMToken.Ok && it.token == RET }) {
                iTokens += RET.iasm

                pos++
            }

            val end = pos.toFloat()

            iTokens = resolveStartAndEnd(iTokens, start, end)
        }
        finally {
            memory.pop()
        }

        return iTokens
    }

    override fun visitReturn(node: Node.Return): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        val subNode = node.node

        if (subNode != null) {
            iTokens += visit(subNode)
        }

        iTokens += RET.iasm

        pos++

        return iTokens
    }

    override fun visitExpression(node: Node.Expression): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.node)

        // TODO: Temporary Debug Code
        iTokens += PEEK.iasm

        iTokens += POP.iasm

        pos += 2 // TODO: pos++

        return iTokens
    }

    override fun visitValue(node: Node.Value): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += PUSH.iasm
        iTokens += ASMToken.Value(node.value.value).iasm

        pos += 2

        return iTokens
    }

    override fun visitName(node: Node.Name): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        val (mode, address) = memory
            .getVariable(node)

        iTokens += load(mode)
        iTokens += ASMToken.Value(address.toFloat()).iasm

        pos += 2

        return iTokens
    }

    override fun visitUnary(node: Node.Unary): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.operand)

        iTokens += when (node.operator) {
            DASH        -> NEG.iasm

            EXCLAMATION -> NOT.iasm

            else        -> error("Not a binary operator '${node.operator}'.")
        }

        pos++

        return iTokens
    }

    override fun visitBinary(node: Node.Binary): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        val left = node.operandLeft
        val right = node.operandRight

        val additional = when (node.operator) {
            PLUS              -> {
                if ((left is Node.Value && left.value.value == 0F) || (right is Node.Value && right.value.value == 0F)) {
                    return iTokens
                }

                listOf(ADD.iasm)
            }

            DASH              -> listOf(SUB.iasm)

            STAR              -> listOf(MUL.iasm)

            SLASH             -> listOf(DIV.iasm)

            PERCENT           -> listOf(MOD.iasm)

            LESS              -> listOf(GEQ.iasm, NOT.iasm)

            LESS_EQUAL        -> listOf(GRT.iasm, NOT.iasm)

            GREATER           -> listOf(GRT.iasm)

            GREATER_EQUAL     -> listOf(GEQ.iasm)

            DOUBLE_AMPERSAND  -> listOf(AND.iasm)

            DOUBLE_PIPE       -> listOf(OR.iasm)

            DOUBLE_EQUAL      -> listOf(EQU.iasm)

            EXCLAMATION_EQUAL -> listOf(EQU.iasm, NOT.iasm)

            else              -> error("Not a binary operator '${node.operator}'.")
        }

        iTokens += visit(node.operandLeft)
        iTokens += visit(node.operandRight)
        iTokens += additional

        pos += additional.size

        return iTokens
    }

    override fun visitAssign(node: Node.Assign): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.node)

        val (_, address) = memory
            .getVariable(node.name)

        iTokens += DUP.iasm
        iTokens += STORE.iasm
        iTokens += ASMToken.Value(address.toFloat()).iasm

        pos += 3

        return iTokens
    }

    override fun visitInvoke(node: Node.Invoke): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        for (arg in node.args.reversed()) {
            iTokens += visit(arg)
        }

        val address = memory
            .getFunction(node.name)

        iTokens += CALL.iasm
        iTokens += ASMToken.Value(address.toFloat()).iasm

        pos += 2

        return iTokens
    }

    private class Memory {
        private val scopes = Stack<Scope>()

        private val global = Scope()

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

        fun addVariable(name: Node.Name) {
            val scope = peek()

            if (scope.addVariable(name)) return

            error("Redeclared variable '${name.name.value}' @ ${name.location}!")
        }

        fun getVariable(name: Node.Name): Lookup {
            var here: Scope? = peek()

            while (here != null && here != global) {
                val variable = here.getVariable(name)

                if (variable != null) return Lookup(Lookup.Mode.LOCAL, variable)

                here = here.parent
            }

            val variable = global.getVariable(name)

            if (variable != null) return Lookup(Lookup.Mode.GLOBAL, variable)

            error("Undeclared variable '${name.name.value}' @ ${name.location}!")
        }

        fun addFunction(name: Node.Name, pos: Int): Lookup.Mode {
            if (peek().addFunction(name, pos)) return Lookup.Mode.LOCAL

            if (global.addFunction(name, pos)) return Lookup.Mode.GLOBAL

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

            val variables = mutableMapOf<String, Int>()
            val functions = mutableMapOf<String, Int>()

            fun addVariable(name: Node.Name): Boolean {
                if (name.name.value in variables) return false

                variables[name.name.value] = variableID++

                return true
            }

            fun getVariable(name: Node.Name) =
                variables[name.name.value]

            fun addFunction(name: Node.Name, pos: Int): Boolean {
                if (name.name.value in functions) return false

                functions[name.name.value] = pos

                return true
            }

            fun getFunction(name: Node.Name) =
                functions[name.name.value]
        }

        data class Lookup(val mode: Mode, val address: Int) {
            enum class Mode {
                GLOBAL,
                LOCAL
            }
        }
    }
}