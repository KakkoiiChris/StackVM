package kakkoiichris.stackvm.asm

import kakkoiichris.stackvm.lang.IASMToken

interface ASMToken {
    val value: Float

    val iasm get() = IASMToken.Ok(this)

    enum class Keyword : ASMToken {
        HALT,
        PUSH,
        POP,
        DUP,
        ADD,
        SUB,
        MUL,
        DIV,
        MOD,
        NEG,
        AND,
        OR,
        NOT,
        EQU,
        GRT,
        GEQ,
        JMP,
        JIF,
        LOAD,
        STORE,
        CALL,
        RET,
        SYS;

        override val value get() = ordinal.toFloat()
    }

    data class Value(override val value: Float) : ASMToken {
        override fun toString() =
            value.toString()
    }

    data object End : ASMToken {
        override val value = 0F
    }
}