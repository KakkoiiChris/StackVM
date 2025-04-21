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

import kakkoiichris.svml.lang.Allocator
import kakkoiichris.svml.lang.Source
import kakkoiichris.svml.lang.lexer.Lexer
import kakkoiichris.svml.lang.parser.Parser
import kakkoiichris.svml.linker.Linker
import kakkoiichris.svml.util.toAddress
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BytecodeFormatter(private val file: File) {
    private val bytecodes: List<Bytecode>

    private var pos = 0

    private var address = 0

    init {
        val source = Source.of(file)

        val lexer = Lexer(source)

        Linker.link()

        val parser = Parser(lexer)

        val program = parser.parse()

        Allocator.allocate(program)

        val compiler = Compiler(program, optimize = true, generateComments = true)

        bytecodes = compiler.convert()
    }

    private fun fetch(): Bytecode {
        val bytecode = bytecodes[pos++]

        address++

        return bytecode
    }

    fun format(): String {
        val lines = mutableListOf<Line>()

        while (pos < bytecodes.size) {
            val here = address

            lines += when (val bytecode = fetch()) {
                is Bytecode.Comment     -> {
                    address--

                    Line(bytecode.toString(), -1)
                }

                is Bytecode.Instruction -> {
                    println(bytecode)
                    getLine(here, bytecode)
                }

                else                    -> error("Invalid start of line '$bytecode' at '$pos'!")
            }
        }

        val maxLength = lines.filter { it.pos >= 0 }.maxBy { it.text.length }.text.length

        val dateTime = LocalDateTime.now()

        val format = DateTimeFormatter.ofPattern("EEE, MMM dd, YYYY, hh:mm:ss a")

        return buildString {
            appendLine("; Bytecode for ${file.name}")
            appendLine("; Created on ${dateTime.format(format)}")

            for ((text, pos) in lines) {
                append(text)

                if (pos >= 0) {
                    append(" ".repeat(maxLength - text.length))

                    append(" ; ${pos.toAddress()}")
                }

                appendLine()
            }
        }
    }

    private fun getLine(pos: Int, instruction: Bytecode.Instruction) = when (instruction.arity) {
        0    -> noArgLine(pos, instruction)

        1    -> oneArgLine(pos, instruction)

        2    -> twoArgLine(pos, instruction)

        else -> error("Invalid instruction arity!")
    }

    private fun noArgLine(pos: Int, instruction: Bytecode.Instruction) =
        Line("$instruction", pos)

    private fun oneArgLine(pos: Int, instruction: Bytecode.Instruction) =
        Line("$instruction ${fetch()}", pos)

    private fun twoArgLine(pos: Int, instruction: Bytecode.Instruction) =
        Line("$instruction ${fetch()} ${fetch()}", pos)

    data class Line(val text: String, val pos: Int)
}