/*   ______  ____   ____  ____    ____  _____
 * .' ____ \|_  _| |_  _||_   \  /   _||_   _|
 * | (___ \_| \ \   / /    |   \/   |    | |
 *  _.____`.   \ \ / /     | |\  /| |    | |   _
 * | \____) |   \ ' /     _| |_\/_| |_  _| |__/ |
 *  \______.'    \_/     |_____||_____||________|
 *
 *         Stack Virtual Machine Language
 *     Copyright (C) 2024 Christian Alexander
 */
package kakkoiichris.svml.lang.compiler

import kakkoiichris.svml.util.truncate

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
        INC,
        DEC,
        AND,
        OR,
        NOT,
        EQU,
        GRT,
        GEQ,
        JMP(1),
        JIF(1),
        GLOB,
        LOD,
        ALOD,
        HLOD,
        HALOD,
        STO,
        ASTO,
        HSTO,
        HASTO,
        SIZE,
        ASIZE,
        HSIZE,
        HASIZE,
        ALLOC(1),
        REALLOC(1),
        FREE(1),
        CALL(1),
        RET,
        FRAME(1),
        ARG,
        SYS(1);

        override val value get() = ordinal.toDouble()
    }

    data class Value(override val value: Double) : Bytecode {
        override fun toString() =
            value.truncate()
    }

    data class Comment(val message: String) : Bytecode {
        override fun toString() =
            "; $message"
    }

    data object End : Bytecode
}