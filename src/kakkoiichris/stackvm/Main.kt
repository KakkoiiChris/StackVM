package kakkoiichris.stackvm

import kakkoiichris.stackvm.cpu.CPU
import kakkoiichris.stackvm.lang.ASMConverter
import kakkoiichris.stackvm.lang.Lexer
import kakkoiichris.stackvm.lang.Parser
import kotlin.time.measureTimedValue

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
//    CPU.debug = true

    while (true) {
        print("> ")

        val src = readln().takeIf { it.isNotEmpty() } ?: break

        val (tokens, compileTime) = measureTimedValue {
            val lexer = Lexer(src)

            val parser = Parser(lexer, false)

            val converter = ASMConverter(parser, false)

            converter.convert()
        }

        println("Compiled: ${compileTime.inWholeMilliseconds / 1E3}s")

        for ((i, token) in tokens.withIndex()) {
            System.out.printf("%02d) %s%n", i, token)
        }

        CPU.load(tokens.iterator())

        val (result, runTime) = measureTimedValue { CPU.run() }

        println("\n< $result (${runTime.inWholeNanoseconds / 1E9}s)\n")
    }
}