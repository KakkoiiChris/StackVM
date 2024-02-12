package kakkoiichris.stackvm.asm

import kakkoiichris.stackvm.asm.ASMToken.Instruction.*
import kakkoiichris.stackvm.util.truncate

object ASMFormatter {
    fun format(values: FloatArray) = buildString {
        var i = 0

        fun fetch() =
            values[i++]

        while (i < values.size) {
            val pos = i

            val value = fetch()

            val instruction = ASMToken.Instruction.entries[value.toInt()]

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

            appendLine("; %03d".format(pos))
        }
    }
}