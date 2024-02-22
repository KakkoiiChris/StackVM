package kakkoiichris.stackvm.lang.compiler

import kakkoiichris.stackvm.util.truncate

interface Bytecode {
    val value get() = 0.0

    val ok get() = Token.Ok(this)

    enum class Instruction(val arity: Int = 0) : Bytecode {
        HALT,
        PUSH(1),
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
        JMP(1),
        JIF(1),
        GLOB,
        LOD(1),
        ALOD(1),
        ILOD(2),
        IALOD(2),
        STO(1),
        ASTO(1),
        ISTO(2),
        IASTO(2),
        ALLOC(1),
        REALLOC(1),
        FREE(1),
        HALOD(1),
        HILOD(2),
        HIALOD(2),
        HASTO(1),
        HISTO(2),
        HIASTO(2),
        SIZE(1),
        HSIZE(1),
        CALL(1),
        RET,
        FRAME(1),
        SYS(1);

        override val value get() = ordinal.toDouble()
    }

    data class Value(override val value: Double) : Bytecode {
        override fun toString() =
            value.truncate()
    }

    data object End : Bytecode
}