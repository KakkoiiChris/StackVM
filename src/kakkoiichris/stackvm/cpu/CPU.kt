package stackvm.cpu

import stackvm.asm.Lexer
import kotlin.math.pow

object CPU {
    private val memory = FloatArray(2.0.pow(16.0).toInt())

    private var instructionPointer = 0
    private var stackPointer = 0
    private var variablePointer = 0

    fun load(vararg program: Float) {
        memory.fill(0F)

        instructionPointer = 0
        stackPointer = program.size
        variablePointer = memory.size / 2

        for (i in program.indices) {
            memory[i] = program[i]
        }
    }

    fun load(lexer: Lexer) {
        memory.fill(0F)

        instructionPointer = 0
        variablePointer = memory.size / 2

        var i = 0

        for (token in lexer) {
            memory[i++] = token.toFloat()
        }

        stackPointer = i
    }

    private fun fetch() = memory[instructionPointer++]

    private fun push(value: Float) {
        memory[++stackPointer] = value
    }

    private fun pop() = memory[stackPointer--]

    private fun peek() = memory[stackPointer]

    private fun Float.toBool() = this != 0F

    private fun Boolean.toFloat() = if (this) 1F else 0F

    fun run(): Float {
        var running = true

        while (running) {
            when (Instruction.values()[fetch().toInt()]) {
                Instruction.HALT  -> running = false

                Instruction.PUSH  -> push(fetch())

                Instruction.POP   -> pop()

                Instruction.DUP   -> push(peek())

                Instruction.ADD   -> {
                    val b = pop()
                    val a = pop()

                    push(a + b)
                }

                Instruction.SUB   -> {
                    val b = pop()
                    val a = pop()

                    push(a - b)
                }

                Instruction.MUL   -> {
                    val b = pop()
                    val a = pop()

                    push(a * b)
                }

                Instruction.DIV   -> {
                    val b = pop()
                    val a = pop()

                    push(a / b)
                }

                Instruction.MOD   -> {
                    val b = pop()
                    val a = pop()

                    push(a % b)
                }

                Instruction.NEG   -> push(-pop())

                Instruction.AND   -> {
                    val b = pop()
                    val a = pop()

                    push((a.toBool() && b.toBool()).toFloat())
                }

                Instruction.OR    -> {
                    val b = pop()
                    val a = pop()

                    push((a.toBool() || b.toBool()).toFloat())
                }

                Instruction.NOT   -> push((!pop().toBool()).toFloat())

                Instruction.EQU   -> {
                    val b = pop()
                    val a = pop()

                    push((a == b).toFloat())
                }

                Instruction.GRT   -> {
                    val b = pop()
                    val a = pop()

                    push((a > b).toFloat())
                }

                Instruction.GEQ   -> {
                    val b = pop()
                    val a = pop()

                    push((a >= b).toFloat())
                }

                Instruction.JMP   -> instructionPointer = fetch().toInt()

                Instruction.JIF   -> {
                    if (pop().toBool()) {
                        instructionPointer = fetch().toInt()
                    }
                }

                Instruction.LOAD  -> push(memory[fetch().toInt() + variablePointer])

                Instruction.STORE -> memory[fetch().toInt() + variablePointer] = pop()
            }
        }

        return pop()
    }
}