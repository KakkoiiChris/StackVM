package kakkoiichris.stackvm.lang.compiler

import kakkoiichris.stackvm.lang.compiler.Bytecode.Instruction.*
import kakkoiichris.stackvm.util.toAddress
import kakkoiichris.stackvm.util.truncate

object BytecodeFormatter {
    fun format(values: FloatArray) = buildString {
        var i = 0

        fun fetch() =
            values[i++]

        while (i < values.size) {
            val pos = i

            val value = fetch()

            val instruction = Bytecode.Instruction.entries[value.toInt()]

            append(instruction)

            append(
                when (instruction) {
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
                    SYS     -> " ${fetch().truncate()}"

                    ILOAD,
                    IALOAD,
                    ISTORE,
                    IASTORE -> " ${fetch().truncate()} ${fetch().truncate()}"

                    else    -> ""
                }
            )

            appendLine("; ${pos.toAddress()}")
        }
    }
}