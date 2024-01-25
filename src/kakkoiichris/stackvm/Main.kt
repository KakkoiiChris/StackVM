package kakkoiichris.stackvm

import kakkoiichris.stackvm.asm.ASMFormatter
import kakkoiichris.stackvm.compiler.ASMConverter
import kakkoiichris.stackvm.compiler.Compiler
import kakkoiichris.stackvm.cpu.CPU1
import kakkoiichris.stackvm.cpu.Debug
import kakkoiichris.stackvm.lang.Lexer
import kakkoiichris.stackvm.lang.Parser
import kakkoiichris.stackvm.util.length
import kakkoiichris.stackvm.util.truncate
import java.io.*
import kotlin.time.measureTimedValue

val cpu = CPU1

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

            "-c" -> {
                mode = Mode.COMPILE
                srcFile = args[a++]
                dstFile = args[a++]
            }

            "-r" -> {
                mode = Mode.RUN
                srcFile = args[a++]
            }

            "-f" -> {
                mode = Mode.FORMAT
                srcFile = args[a++]
                dstFile = args[a++]
            }
        }
    }

    when (mode) {
        Mode.REPL    -> repl()

        Mode.COMPILE -> compile(srcFile, dstFile)

        Mode.RUN     -> run(srcFile)

        Mode.FORMAT  -> format(srcFile, dstFile)
    }
}

private enum class Mode {
    REPL,
    COMPILE,
    RUN,
    FORMAT
}

private fun repl() {
    while (true) {
        print("> ")

        val src = readln().takeIf { it.isNotEmpty() } ?: break

        try {
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

            cpu.load(tokens.iterator())

            val (result, runTime) = measureTimedValue { cpu.run() }

            println("\n< ${result.truncate()} (${runTime.inWholeNanoseconds / 1E9}s)\n")
        }
        catch (e: IllegalStateException) {
            e.printStackTrace()
        }
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

    val values = FloatArray(length)

    for (i in 0 until length) {
        values[i] = `in`.readFloat()
    }

    cpu.load(values)

    val (result, runTime) = measureTimedValue { cpu.run() }

    println("\n< ${result.truncate()} (${runTime.inWholeNanoseconds / 1E9}s)\n")
}

private fun format(srcName: String, dstName: String) {
    val srcFile = File(srcName)

    if (!srcFile.exists()) error("Cannot load source file!")

    val src = srcFile.readText()

    val values = Compiler.compile(src)

    val dstFile = File(dstName)

    dstFile.delete()
    if (!dstFile.createNewFile()) error("Cannot create destination file!")

    val out = BufferedWriter(FileWriter(dstFile))

    out.write(ASMFormatter.format(values))

    out.close()
}