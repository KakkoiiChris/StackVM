package stackvm

import stackvm.asm.Lexer
import stackvm.cpu.CPU

/**
 * Stack VM
 *
 * Copyright (C) 2023, KakkoiiChris
 *
 * File:    Main.kt
 *
 * Created: Saturday, June 10, 2023, 01:14:46
 *
 * @author Christian Bryce Alexander
 */
fun main() {
    val src = """
        ;JMP 10
        PUSH 5
        PUSH 3
        MUL
        DUP
        ADD
        NEG
        JMP 12
        PUSH 5
        HALT
    """.trimIndent()

    val lexer = Lexer(src)

    CPU.load(lexer)

    println(CPU.run())
}