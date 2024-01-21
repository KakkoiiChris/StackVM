package kakkoiichris.stackvm.asm

import kakkoiichris.stackvm.compiler.IASMToken
import kakkoiichris.stackvm.util.truncate

interface ASMToken {
    val value: Float

    val iasm get() = IASMToken.Ok(this)

    enum class Instruction : ASMToken {
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
        LOADG,
        LOAD,
        STORE,
        CALL,
        RET,
        FRAME,
        SYS;

        override val value get() = ordinal.toFloat()
    }

    data class Value(override val value: Float) : ASMToken {
        override fun toString() =
            value.truncate()
    }

    data object End : ASMToken {
        override val value = 0F
    }
}