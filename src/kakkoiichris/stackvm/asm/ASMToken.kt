package kakkoiichris.stackvm.asm

interface ASMToken {
    val value: Float

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
        STORE;

        override val value get() = ordinal.toFloat()
    }

    data class Value(override val value: Float) : ASMToken

    data object End : ASMToken {
        override val value = 0F
    }
}