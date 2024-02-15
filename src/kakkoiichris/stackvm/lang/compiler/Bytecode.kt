package kakkoiichris.stackvm.lang.compiler

import kakkoiichris.stackvm.util.truncate

interface Bytecode {
    val value: Float

    val intermediate get() = Token.Ok(this)

    enum class Instruction : Bytecode {
        HALT,
        PUSH,
        POP,
        DUP,
        ADD,
        SUB,
        MUL,
        DIV,
        IDIV,
        MOD,
        IMOD,
        NEG,
        AND,
        OR,
        NOT,
        EQU,
        GRT,
        GEQ,
        JMP,
        JIF,
        GLOBAL,
        LOAD,
        ALOAD,
        ILOAD,
        IALOAD,
        STORE,
        ASTORE,
        ISTORE,
        IASTORE,
        SIZE,
        CALL,
        RET,
        FRAME,
        SYS;

        override val value get() = ordinal.toFloat()
    }

    data class Value(override val value: Float) : Bytecode {
        override fun toString() =
            value.truncate()
    }

    data object End : Bytecode {
        override val value = 0F
    }
}