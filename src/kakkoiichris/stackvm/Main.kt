package kakkoiichris.stackvm

import kakkoiichris.stackvm.cpu.CPU
import kakkoiichris.stackvm.cpu.Debug
import kakkoiichris.stackvm.lang.ASMConverter
import kakkoiichris.stackvm.lang.Lexer
import kakkoiichris.stackvm.lang.Parser
import kakkoiichris.stackvm.util.length
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
fun main(args: Array<String>) {
    var a = 0

    while (a < args.size) {
        when (args[a++].lowercase()) {
            "-d" -> Debug.enabled = true
        }
    }

    while (true) {
        print("> ")

        val src = readln().takeIf { it.isNotEmpty() } ?: break

        val (tokens, compileTime) = measureTimedValue {
            val lexer = Lexer(src)

            val parser = Parser(lexer, false)

            val converter = ASMConverter(parser, false)

            converter.convert()
        }

        Debug {
            println("Compiled: ${compileTime.inWholeMilliseconds / 1E3}s\n")

            val max = tokens.size.length()

            for ((i, token) in tokens.withIndex()) {
                println("%0${max}d) %s".format(i, token))
            }

            println()
        }

        CPU.load(tokens.iterator())

        val (result, runTime) = measureTimedValue { CPU.run() }

        println("\n< $result (${runTime.inWholeNanoseconds / 1E9}s)\n")
    }
}