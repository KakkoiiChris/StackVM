package kakkoiichris.stackvm.lang

import kakkoiichris.stackvm.asm.ASMToken
import kakkoiichris.stackvm.asm.ASMToken.Keyword.*
import kakkoiichris.stackvm.lang.TokenType.Symbol.*
import java.util.*

class ASMConverter(private val parser: Parser, private val optimize: Boolean) : Node.Visitor<List<IASMToken>> {
    private var pos = 0

    private val memory = Memory()

    fun convert(): List<ASMToken> {
        try {
            memory.push()

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
            memory.pop()
        }
    }

    private fun resolve(iTokens: List<IASMToken>): MutableList<IASMToken> {
        val start = memory.start.toFloat()
        val end = memory.end.toFloat()

        return iTokens
            .map { it.resolve(start, end) }
            .toMutableList()
    }

    override fun visitVar(node: Node.Var): List<IASMToken> {
        val iTokens = mutableListOf<IASMToken>()

        iTokens += visit(node.node)

        memory.addVariable(node.name)
        val address = memory.getVariable(node.name)

        iTokens += STORE.iasm
        iTokens += ASMToken.Value(address.toFloat()).iasm

        pos += 2

        return iTokens
    }

    override fun visitIf(node: Node.If): List<IASMToken> {
        var iTokens = mutableListOf<IASMToken>()

        try {
            memory.push()

            memory.start = pos

            iTokens += visit(node.condition)

            iTokens += NOT.iasm
            iTokens += JIF.iasm
            iTokens += IASMToken.AwaitEnd()

            pos += 3

            for (stmt in node.body) {
                iTokens += visit(stmt)
            }

            memory.end = pos

            iTokens = resolve(iTokens)
        }
        finally {
            memory.pop()
        }

        return iTokens
    }

    override fun visitWhile(node: Node.While): List<IASMToken> {
        var iTokens = mutableListOf<IASMToken>()

        try {
            memory.push()

            memory.start = pos

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

            memory.end = pos

            iTokens = resolve(iTokens)
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

            memory.start = pos

            for (param in node.params) {
                memory.addVariable(param)

                val address = memory.getVariable(param)

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

            memory.end = pos

            iTokens = resolve(iTokens)
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

        val address = memory.getVariable(node)

        iTokens += LOAD.iasm
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

        val address = memory.getVariable(node.name)

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

        val address = memory.getFunction(node.name)

        iTokens += CALL.iasm
        iTokens += ASMToken.Value(address.toFloat()).iasm

        pos += 2

        return iTokens
    }

    private class Memory {
        private val scopes = Stack<Scope>()

        fun push() {
            if (scopes.isNotEmpty()) {
                val scope = peek()

                val next = Scope(scope.variableID)

                scopes.push(next)
            }
            else {
                scopes.push(Scope())
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

        var start: Int
            get() = peek().start
            set(start) {
                peek().start = start
            }

        var end: Int
            get() = peek().end
            set(end) {
                peek().end = end
            }

        fun addVariable(name: Node.Name) {
            peek().addVariable(name)
        }

        fun getVariable(name: Node.Name) =
            peek().getVariable(name)

        fun addFunction(name: Node.Name, pos: Int) {
            peek().addFunction(name, pos)
        }

        fun getFunction(name: Node.Name) =
            peek().getFunction(name)

        class Scope(var variableID: Int = 0) {
            var start = -1
            var end = -1

            val variables = mutableMapOf<String, Int>()
            val functions = mutableMapOf<String, Int>()

            fun addVariable(name: Node.Name) {
                if (name.name.value in variables) error("Redeclared variable '${name.name.value}'!")

                variables[name.name.value] = variableID++
            }

            fun getVariable(name: Node.Name): Int {
                if (name.name.value !in variables) error("Undeclared variable '${name.name.value}'!")

                return variables[name.name.value]!!
            }

            fun addFunction(name: Node.Name, pos: Int) {
                if (name.name.value in functions) error("Redeclared function '${name.name.value}'!")

                functions[name.name.value] = pos
            }

            fun getFunction(name: Node.Name): Int {
                if (name.name.value !in functions) error("Undeclared function '${name.name.value}'!")

                return functions[name.name.value]!!
            }
        }
    }
}