package kakkoiichris.stackvm.asm

import kakkoiichris.stackvm.asm.ASMToken.Instruction.*
import kakkoiichris.stackvm.util.truncate

object ASMFormatter {
    fun format(values: FloatArray) = buildString {
        var i = 0

        fun fetch() =
            values[i++]

        while (i < values.size) {
            val value = fetch()

            val instruction = ASMToken.Instruction.entries[value.toInt()]

            append(instruction)

            appendLine(
                when (instruction) {
                    PUSH, JMP, JIF, LOAD, STORE, CALL, SYS -> " ${fetch().truncate()}"

                    else                                   -> ""
                }
            )
        }
    }
}