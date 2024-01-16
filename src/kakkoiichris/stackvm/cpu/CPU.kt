package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.asm.ASMToken
import kotlin.math.max
import kotlin.math.pow

object CPU {
    private val memory = FloatArray(2.0.pow(16.0).toInt())

    private var instructionPointer = 0
    private var stackPointerOffset = 0
    private var stackPointer = 0
    private var variablePointer = 0

    var debug = false

    fun load(tokenizer: Iterator<ASMToken>) {
        memory.fill(0F)

        instructionPointer = 0
        variablePointer = memory.size / 2

        var i = 0

        for (token in tokenizer) {
            memory[i++] = token.value
        }

        stackPointerOffset = i
    }

    private fun fetch() = memory[instructionPointer++]

    private fun push(value: Float) {
        memory[stackPointerOffset + ++stackPointer] = value
    }

    private fun pop(): Float {
        val value = memory[stackPointerOffset + stackPointer]

        stackPointer = max(stackPointer - 1, 0)

        return value
    }

    private fun peek() = memory[stackPointerOffset + stackPointer]

    private fun Float.toBool() = this != 0F

    private fun Boolean.toFloat() = if (this) 1F else 0F

    fun run(): Float {
        var result = Float.NaN
        var running = true

        while (running) {
            val instruction = Instruction.entries[fetch().toInt()]

            when (instruction) {
                Instruction.HALT  -> {
                    result = pop()

                    running = false
                }

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
                    val address = fetch().toInt()

                    if (pop().toBool()) {
                        instructionPointer = address
                    }
                }

                Instruction.LOAD  -> push(memory[fetch().toInt() + variablePointer])

                Instruction.STORE -> memory[fetch().toInt() + variablePointer] = pop()
            }

            if (debug) {
                print("$instruction\nSTACK: ")

                for (i in stackPointerOffset..(stackPointerOffset + stackPointer)) {
                    print("${memory[i]} ")
                }

                println()
            }
        }

        return result
    }
}