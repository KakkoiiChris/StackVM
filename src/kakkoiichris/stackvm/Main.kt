package kakkoiichris.stackvm

import kakkoiichris.stackvm.cpu.CPU
import kakkoiichris.stackvm.cpu.DebugCPU
import kakkoiichris.stackvm.cpu.ReleaseCPU
import kakkoiichris.stackvm.linker.Linker
import kakkoiichris.stackvm.lang.Allocator
import kakkoiichris.stackvm.lang.Directory
import kakkoiichris.stackvm.lang.Source
import kakkoiichris.stackvm.lang.compiler.BytecodeFormatter
import kakkoiichris.stackvm.lang.compiler.Compiler
import kakkoiichris.stackvm.lang.lexer.Lexer
import kakkoiichris.stackvm.lang.parser.Parser
import kakkoiichris.stackvm.util.truncate
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

private fun compile(srcFile: File): DoubleArray {
    val source = Source.of(srcFile)

    val lexer = Lexer(source)

    Linker.link()

    val parser = Parser(lexer, false)

    val program = parser.parse()

    Allocator.allocate(program)

    val compiler = Compiler(program, true, false)

    return compiler.compile()
}

private fun repl(cpu: CPU) {
    while (true) {
        print("> ")

        val src = readln().takeIf { it.isNotEmpty() } ?: break

        try {
            val (bytecodes, compileTime) = measureTimedValue {
                val lexer = Lexer(Source("<REPL>", src))

                val parser = Parser(lexer, false)

                val program = parser.parse()

                Allocator.allocate(program)

                val compiler = Compiler(program, true, false)

                compiler.convert()
            }

            println("Compiled: ${compileTime.inWholeMilliseconds / 1E3}s\n")

            cpu.initialize(bytecodes.iterator())

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

    Directory.root = srcFile.parentFile

    if (!srcFile.exists()) error("Cannot load source file!")

    val values = compile(srcFile)

    val dstFile = File(dstName)

    if (!dstFile.exists()) {
        if (!dstFile.createNewFile()) {
            error("Cannot create destination file!")
        }
    }

    val out = DataOutputStream(BufferedOutputStream(FileOutputStream(dstFile, false)))

    out.writeInt(values.size)

    for (value in values) {
        out.writeDouble(value)
    }

    out.close()
}


private fun runFile(cpu: CPU, srcName: String) {
    val srcFile = File(srcName)

    val `in` = DataInputStream(BufferedInputStream(FileInputStream(srcFile)))

    val length = `in`.readInt()

    val values = DoubleArray(length)

    for (i in 0 until length) {
        values[i] = `in`.readDouble()
    }

    cpu.initialize(values)

    Linker.link()

    val (result, runTime) = measureTimedValue { cpu.run() }

    println("\n< ${result.truncate()} (${runTime.inWholeNanoseconds / 1E9}s)\n")
}

private fun formatFile(srcName: String, dstName: String) {
    val srcFile = File(srcName)

    Directory.root = srcFile.parentFile

    if (!srcFile.exists()) error("Cannot load source file!")

    val formatter = BytecodeFormatter(srcFile)

    val format = formatter.format()

    val dstFile = File(dstName)

    if (!dstFile.exists()) {
        if (!dstFile.createNewFile()) {
            error("Cannot create destination file!")
        }
    }

    val out = BufferedWriter(FileWriter(dstFile, false))

    out.write(format)

    out.close()
}