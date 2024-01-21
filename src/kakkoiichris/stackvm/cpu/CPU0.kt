package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.asm.ASMToken
import kakkoiichris.stackvm.asm.ASMToken.Instruction
import kakkoiichris.stackvm.util.toBool
import kakkoiichris.stackvm.util.toFloat
import kakkoiichris.stackvm.util.truncate
import kotlin.math.max

object CPU0 : CPU() {
    private var instructionPointer = 0

    private var stackPointerOrigin = 0
    private var stackPointerOffset = 0
    private val stackPointer get() = stackPointerOrigin + stackPointerOffset

    private var variablePointer = 0

    private var callPointerOrigin = 0
    private var callPointerOffset = 0
    private val callPointer get() = callPointerOrigin + callPointerOffset

    fun load(tokenizer: Iterator<ASMToken>) =
        load(tokenizer.asSequence().toList().map { it.value })

    override fun load(values: List<Float>) {
        memory.fill(0F)

        instructionPointer = 0
        variablePointer = memory.size / 3

        callPointerOrigin = 2 * memory.size / 3

        var i = 0

        for (value in values) {
            memory[i++] = value
        }

        stackPointerOrigin = i
    }

    private fun fetch() = memory[instructionPointer++]

    private fun pushStack(value: Float) {
        stackPointerOffset++

        memory[stackPointer] = value
    }

    private fun popStack(): Float {
        val value = memory[stackPointer]

        stackPointerOffset = max(stackPointerOffset - 1, 0)

        return value
    }

    private fun peekStack() = memory[stackPointer]

    private fun pushCall(value: Float) {
        callPointerOffset++

        memory[callPointer] = value
    }

    private fun popCall(): Float {
        val value = memory[callPointer]

        callPointerOffset = max(callPointerOffset - 1, 0)

        return value
    }

    override fun run(): Float {
        var result = Float.NaN
        var running = true

        while (running) {
            val instruction = Instruction.entries[fetch().toInt()]

            when (instruction) {
                Instruction.HALT  -> {
                    result = popStack()

                    running = false
                }

                Instruction.PUSH  -> {
                    val value = fetch()

                    Debug.println("PUSH ${value.truncate()}")

                    pushStack(value)
                }

                Instruction.POP   -> {
                    Debug.println("POP")

                    popStack()
                }

                Instruction.DUP   -> {
                    val value = peekStack()

                    Debug.println("DUP ${value.truncate()}")

                    pushStack(value)
                }

                Instruction.ADD   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("ADD ${a.truncate()} ${b.truncate()}")

                    pushStack(a + b)
                }

                Instruction.SUB   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("SUB ${a.truncate()} ${b.truncate()}")

                    pushStack(a - b)
                }

                Instruction.MUL   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("MUL ${a.truncate()} ${b.truncate()}")

                    pushStack(a * b)
                }

                Instruction.DIV   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("DIV ${a.truncate()} ${b.truncate()}")

                    pushStack(a / b)
                }

                Instruction.MOD   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("MOD ${a.truncate()} ${b.truncate()}")

                    pushStack(a % b)
                }

                Instruction.NEG   -> {
                    val value = popStack()

                    Debug.println("NEG ${value.truncate()}")

                    pushStack(-value)
                }

                Instruction.AND   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("AND ${a.truncate()} ${b.truncate()}")

                    pushStack((a.toBool() && b.toBool()).toFloat())
                }

                Instruction.OR    -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("OR ${a.truncate()} ${b.truncate()}")

                    pushStack((a.toBool() || b.toBool()).toFloat())
                }

                Instruction.NOT   -> {
                    val value = popStack()

                    Debug.println("NOT ${value.truncate()}")

                    pushStack((!value.toBool()).toFloat())
                }

                Instruction.EQU   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("EQU ${a.truncate()} ${b.truncate()}")

                    pushStack((a == b).toFloat())
                }

                Instruction.GRT   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("GRT ${a.truncate()} ${b.truncate()}")

                    pushStack((a > b).toFloat())
                }

                Instruction.GEQ   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("GEQ ${a.truncate()} ${b.truncate()}")

                    pushStack((a >= b).toFloat())
                }

                Instruction.JMP   -> instructionPointer = fetch().toInt()

                Instruction.JIF   -> {
                    val address = fetch().toInt()

                    if (popStack().toBool()) {
                        instructionPointer = address
                    }
                }

                Instruction.LOAD  -> pushStack(memory[fetch().toInt() + variablePointer])

                Instruction.STORE -> memory[fetch().toInt() + variablePointer] = popStack()

                Instruction.CALL  -> {
                    val address = instructionPointer + 1
                    instructionPointer = fetch().toInt()

                    pushCall(address.toFloat())
                }

                Instruction.RET   -> {
                    val address = popCall()

                    instructionPointer = address.toInt()
                }

                else              -> TODO("Instruction $instruction is not implemented.")
            }

            Debug {
                print("STACK: ")

                for (i in stackPointerOrigin..stackPointer) {
                    print("${memory[i].truncate()} ")
                }

                println()
            }
        }

        return result
    }
}