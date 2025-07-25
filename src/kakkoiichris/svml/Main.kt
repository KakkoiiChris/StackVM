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
package kakkoiichris.svml

import kakkoiichris.svml.cpu.CPU
import kakkoiichris.svml.cpu.DebugCPU
import kakkoiichris.svml.cpu.ReleaseCPU
import kakkoiichris.svml.lang.Allocator
import kakkoiichris.svml.lang.Directory
import kakkoiichris.svml.lang.Semantics
import kakkoiichris.svml.lang.Source
import kakkoiichris.svml.lang.compiler.BytecodeFormatter
import kakkoiichris.svml.lang.compiler.Compiler
import kakkoiichris.svml.lang.lexer.Lexer
import kakkoiichris.svml.lang.parser.Parser
import kakkoiichris.svml.linker.Linker
import kakkoiichris.svml.util.SVMLError
import kakkoiichris.svml.util.truncate
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
            "-c" -> {
                mode = Mode.COMPILE
                srcFile = args[a++]
                dstFile = args[a++]
            }

            "-d" -> cpu = DebugCPU

            "-f" -> {
                mode = Mode.FORMAT
                srcFile = args[a++]
                dstFile = args[a++]
            }

            "-r" -> {
                mode = Mode.RUN
                srcFile = args[a++]
            }

            "-v" -> {
                mode = Mode.VISUAL
                srcFile = args[a++]
            }
        }
    }

    try {
        when (mode) {
            Mode.REPL    -> repl(cpu)

            Mode.COMPILE -> compileFile(srcFile, dstFile)

            Mode.RUN     -> runFile(cpu, srcFile)

            Mode.VISUAL  -> visualizeFile(srcFile)

            Mode.FORMAT  -> formatFile(srcFile, dstFile)
        }
    }
    catch (e: SVMLError) {
        System.err.println(e.message)

        e.printStackTrace()
    }
}

private enum class Mode {
    REPL,
    COMPILE,
    RUN,
    VISUAL,
    FORMAT
}

private fun compile(srcFile: File): DoubleArray {
    val source = Source.of(srcFile)

    val lexer = Lexer(source)

    Linker.link()

    val parser = Parser(lexer)

    val program = parser.parse()

    Semantics.check(program)

    Allocator.allocate(program)

    val compiler = Compiler(program, optimize = true, generateComments = false)

    return compiler.compile()
}

private fun repl(cpu: CPU) {
    while (true) {
        print("> ")

        val src = readln().takeIf { it.isNotEmpty() } ?: break

        try {
            val (bytecodes, compileTime) = measureTimedValue {
                val lexer = Lexer(Source("<REPL>", src))

                val parser = Parser(lexer)

                val program = parser.parse()

                Allocator.allocate(program)

                val compiler = Compiler(program, optimize = true, generateComments = false)

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

    if (!dstFile.exists() && !dstFile.createNewFile()) {
        error("Cannot create destination file!")
    }

    val out = DataOutputStream(BufferedOutputStream(FileOutputStream(dstFile, false)))

    out.writeInt(values.size)

    for (value in values) {
        out.writeDouble(value)
    }

    out.close()
}

private fun visualizeFile(srcName: String) {
    val srcFile = File(srcName)

    val `in` = DataInputStream(BufferedInputStream(FileInputStream(srcFile)))

    val length = `in`.readInt()

    val values = DoubleArray(length)

    for (i in 0 until length) {
        values[i] = `in`.readDouble()
    }

    //val cpu = VisualizerCPU(srcName)

    //cpu.initialize(values)

    //Linker.link()

    //val result = cpu.run()

    //println("\n< ${result.truncate()} (${runTime.inWholeNanoseconds / 1E9}s)\n")
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

    if (!dstFile.exists() && !dstFile.createNewFile()) {
        error("Cannot create destination file!")
    }

    val out = BufferedWriter(FileWriter(dstFile, false))

    out.write(format)

    out.close()
}