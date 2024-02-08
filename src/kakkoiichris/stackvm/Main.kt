package kakkoiichris.stackvm

import kakkoiichris.stackvm.asm.ASMFormatter
import kakkoiichris.stackvm.compiler.Compiler
import kakkoiichris.stackvm.cpu.CPU
import kakkoiichris.stackvm.cpu.DebugCPU
import kakkoiichris.stackvm.cpu.ReleaseCPU
import kakkoiichris.stackvm.lang.Lexer
import kakkoiichris.stackvm.lang.MemoryAllocator
import kakkoiichris.stackvm.lang.Parser
import kakkoiichris.stackvm.util.truncate
import java.io.*
import kotlin.math.abs
import kotlin.math.log10
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
    var cpu: CPU = ReleaseCPU
    var mode = Mode.REPL
    var srcFile = ""
    var dstFile = ""

    var a = 0

    while (a < args.size) {
        when (args[a++].lowercase()) {
            "-d" -> {
                cpu = DebugCPU
            }

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
        Mode.REPL    -> repl(cpu)

        Mode.COMPILE -> compileFile(srcFile, dstFile)

        Mode.RUN     -> runFile(cpu, srcFile)

        Mode.FORMAT  -> formatFile(srcFile, dstFile)
    }
}

private enum class Mode {
    REPL,
    COMPILE,
    RUN,
    FORMAT
}

private fun compile(src: String): FloatArray {
    val lexer = Lexer(src)

    val parser = Parser(lexer, false)

    val program = parser.parse()

    MemoryAllocator.allocate(program)

    val compiler = Compiler(program, false)

    return compiler.compile()
}

private fun Int.length() = when {
    this == 0 -> 1

    this < 0  -> log10(abs(toFloat())).toInt() + 2

    else      -> log10(abs(toFloat())).toInt() + 1
}

private fun repl(cpu: CPU) {
    while (true) {
        print("> ")

        val src = readln().takeIf { it.isNotEmpty() } ?: break

        try {
            val (tokens, compileTime) = measureTimedValue {
                val lexer = Lexer(src)

                val parser = Parser(lexer, false)

                val program = parser.parse()

                MemoryAllocator.allocate(program)

                val compiler = Compiler(program, false)

                compiler.convert()
            }

            println("Compiled: ${compileTime.inWholeMilliseconds / 1E3}s\n")

            cpu.initialize(tokens.iterator())

            val (result, runTime) = measureTimedValue { cpu.run() }

            println("\n< ${result.truncate()} (${runTime.inWholeNanoseconds / 1E9}s)\n")
        }
        catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }
}

private fun compileFile(srcName: String, dstName: String) {
    val srcFile = File(srcName)

    if (!srcFile.exists()) error("Cannot load source file!")

    val src = srcFile.readText()

    val values = compile(src)

    val dstFile = File(dstName)

    if (!dstFile.exists()) {
        if (!dstFile.createNewFile()) {
            error("Cannot create destination file!")
        }
    }

    val out = DataOutputStream(BufferedOutputStream(FileOutputStream(dstFile, false)))

    out.writeInt(values.size)

    for (value in values) {
        out.writeFloat(value)
    }

    out.close()
}


private fun runFile(cpu: CPU, srcName: String) {
    val srcFile = File(srcName)

    val `in` = DataInputStream(BufferedInputStream(FileInputStream(srcFile)))

    val length = `in`.readInt()

    val values = FloatArray(length)

    for (i in 0 until length) {
        values[i] = `in`.readFloat()
    }

    cpu.initialize(values)

    val (result, runTime) = measureTimedValue { cpu.run() }

    println("\n< ${result.truncate()} (${runTime.inWholeNanoseconds / 1E9}s)\n")
}

private fun formatFile(srcName: String, dstName: String) {
    val srcFile = File(srcName)

    if (!srcFile.exists()) error("Cannot load source file!")

    val src = srcFile.readText()

    val values = compile(src)

    val dstFile = File(dstName)

    dstFile.delete()
    if (!dstFile.createNewFile()) error("Cannot create destination file!")

    val out = BufferedWriter(FileWriter(dstFile))

    out.write(ASMFormatter.format(values))

    out.close()
}