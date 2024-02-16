package kakkoiichris.stackvm.lang.compiler

import kakkoiichris.stackvm.lang.Allocator
import kakkoiichris.stackvm.lang.Source
import kakkoiichris.stackvm.lang.compiler.Bytecode.Instruction.*
import kakkoiichris.stackvm.lang.lexer.Lexer
import kakkoiichris.stackvm.lang.parser.Parser
import kakkoiichris.stackvm.util.toAddress
import kakkoiichris.stackvm.util.truncate
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BytecodeFormatter(private val file: File) {
    private val values: FloatArray

    private var pos = 0

    init {
        val source = Source.of(file)

        val lexer = Lexer(source)

        val parser = Parser(lexer, false)

        val program = parser.parse()

        Allocator.allocate(program)

        val compiler = Compiler(program, false)

        values = compiler.compile()
    }

    private fun fetch() =
        values[pos++]

    private fun fetchInt() =
        fetch().toInt()

    fun format(): String {
        val lines = mutableListOf<Line>()

        while (pos < values.size) {
            val here = pos

            val opCode = fetchInt()

            val instruction = Bytecode.Instruction.entries[opCode]

            lines += getLine(here, instruction)
        }

        val maxLength = lines.maxBy { it.text.length }.text.length

        val dateTime = LocalDateTime.now()
        
        val format = DateTimeFormatter.ofPattern("EEE, MMM dd, YYYY, hh:mm:ss a")

        return buildString {
            appendLine("; Bytecode for ${file.name}")
            appendLine("; Created on ${dateTime.format(format)}")

            for ((text, pos) in lines) {
                append(text)

                append(" ".repeat(maxLength - text.length))

                appendLine(" ; ${pos.toAddress()}")
            }
        }
    }

    private fun getLine(pos: Int, instruction: Bytecode.Instruction): Line {
        return when (instruction) {
            PUSH,
            JMP,
            JIF,
            LOAD,
            ALOAD,
            STORE,
            ASTORE,
            SIZE,
            CALL,
            FRAME,
            SYS     -> oneArgLine(pos, instruction)

            ILOAD,
            IALOAD,
            ISTORE,
            IASTORE -> twoArgLine(pos, instruction)

            else    -> noArgLine(pos, instruction)
        }
    }

    private fun noArgLine(pos: Int, instruction: Bytecode.Instruction) =
        Line("$instruction", pos)

    private fun oneArgLine(pos: Int, instruction: Bytecode.Instruction) =
        Line("$instruction ${fetch().truncate()}", pos)

    private fun twoArgLine(pos: Int, instruction: Bytecode.Instruction) =
        Line("$instruction ${fetch().truncate()} ${fetch().truncate()}", pos)

    data class Line(val text: String, val pos: Int)
}