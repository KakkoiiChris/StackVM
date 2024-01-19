package kakkoiichris.stackvm

import kakkoiichris.stackvm.cpu.CPU
import kakkoiichris.stackvm.cpu.Debug
import kakkoiichris.stackvm.lang.ASMConverter
import kakkoiichris.stackvm.lang.Compiler
import kakkoiichris.stackvm.lang.Lexer
import kakkoiichris.stackvm.lang.Parser
import kakkoiichris.stackvm.util.length
import java.io.*
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
    var mode = Mode.REPL
    var srcFile = ""
    var dstFile = ""

    var a = 0

    while (a < args.size) {
        when (args[a++].lowercase()) {
            "-d" -> Debug.enabled = true

            "-f" -> {
                mode = Mode.RUN
                srcFile = args[a++]
            }

            "-c" -> {
                mode = Mode.COMPILE
                srcFile = args[a++]
                dstFile = args[a++]
            }
        }
    }

    when (mode) {
        Mode.REPL    -> repl()

        Mode.COMPILE -> compile(srcFile, dstFile)

        Mode.RUN     -> run(srcFile)
    }
}

private enum class Mode {
    REPL,
    COMPILE,
    RUN
}

private fun repl() {
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

private fun compile(srcName: String, dstName: String) {
    val srcFile = File(srcName)

    if (!srcFile.exists()) error("Cannot load source file!")

    val src = srcFile.readText()

    val values = Compiler.compile(src)

    val dstFile = File(dstName)

    if (!dstFile.delete()) error("Cannot delete destination file!")
    if (!dstFile.createNewFile()) error("Cannot create destination file!")

    val out = DataOutputStream(BufferedOutputStream(FileOutputStream(dstFile)))

    out.writeInt(values.size)

    for (value in values) {
        out.writeFloat(value)
    }

    out.close()
}

private fun run(srcName: String) {
    val srcFile = File(srcName)

    val `in` = DataInputStream(BufferedInputStream(FileInputStream(srcFile)))

    val length = `in`.readInt()

    val values = mutableListOf<Float>()

    for (i in 0 until length) {
        values += `in`.readFloat()
    }

    CPU.load(values)

    val (result, runTime) = measureTimedValue { CPU.run() }

    println("\n< $result (${runTime.inWholeNanoseconds / 1E9}s)\n")
}