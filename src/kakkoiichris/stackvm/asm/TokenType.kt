package stackvm.asm

interface TokenType {
    val value: Float
}

enum class Keyword : TokenType {
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

data class Value(override val value: Float) : TokenType

data object End : TokenType {
    override val value = 0F
}