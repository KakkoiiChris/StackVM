package kakkoiichris.stackvm.asm

interface ASMTokenType {
    val value: Float

    enum class Keyword : ASMTokenType {
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

    data class Value(override val value: Float) : ASMTokenType

    data object End : ASMTokenType {
        override val value = 0F
    }
}