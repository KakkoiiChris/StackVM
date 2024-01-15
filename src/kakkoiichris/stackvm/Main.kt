package kakkoiichris.stackvm

import kakkoiichris.stackvm.cpu.CPU
import kakkoiichris.stackvm.lang.ASMConverter
import kakkoiichris.stackvm.lang.Lexer
import kakkoiichris.stackvm.lang.Parser

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
    while (true) {
        print("> ")

        val src = readln().takeIf { it.isNotEmpty() } ?: break

        val lexer = Lexer(src)

        val parser = Parser(lexer)

        val converter = ASMConverter(parser)

        val tokens = converter.convert()

        for ((i,token) in tokens.withIndex()) {
            System.out.printf("%02d) %s%n", i, token)
        }

        CPU.load(tokens.iterator())

        println("\n< ${CPU.run()}\n")
    }
}