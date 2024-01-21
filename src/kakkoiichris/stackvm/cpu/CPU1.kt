package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.asm.ASMToken
import kakkoiichris.stackvm.util.toBool
import kakkoiichris.stackvm.util.toFloat
import kakkoiichris.stackvm.util.truncate

object CPU1 : CPU() {
    private const val IPO_ADR = 0
    private const val IPA_ADR = 1
    private const val SPO_ADR = 2
    private const val SPA_ADR = 3
    private const val VPO_ADR = 4
    private const val VPA_ADR = 5
    private const val CPO_ADR = 6
    private const val CPA_ADR = 7

    private var instructionPointerOrigin: Int
        get() = memory[IPO_ADR].toInt()
        set(value) {
            memory[IPO_ADR] = value.toFloat()
        }

    private var instructionPointer: Int
        get() = memory[IPA_ADR].toInt()
        set(value) {
            memory[IPA_ADR] = value.toFloat()
        }

    private var stackPointerOrigin: Int
        get() = memory[SPO_ADR].toInt()
        set(value) {
            memory[SPO_ADR] = value.toFloat()
        }

    private var stackPointer: Int
        get() = memory[SPA_ADR].toInt()
        set(value) {
            memory[SPA_ADR] = value.toFloat()
        }

    private var variablePointerOrigin: Int
        get() = memory[VPO_ADR].toInt()
        set(value) {
            memory[VPO_ADR] = value.toFloat()
        }

    private var variablePointer: Int
        get() = memory[VPA_ADR].toInt()
        set(value) {
            memory[VPA_ADR] = value.toFloat()
        }

    private var callPointerOrigin: Int
        get() = memory[CPO_ADR].toInt()
        set(value) {
            memory[CPO_ADR] = value.toFloat()
        }

    private var callPointer: Int
        get() = memory[CPA_ADR].toInt()
        set(value) {
            memory[CPA_ADR] = value.toFloat()
        }

    fun load(tokenizer: Iterator<ASMToken>) =
        load(tokenizer.asSequence().toList().map { it.value })

    override fun load(values: List<Float>) {
        memory.fill(0F)

        instructionPointer = 8
        instructionPointerOrigin = instructionPointer

        variablePointer = memory.size / 3
        variablePointerOrigin = variablePointer

        callPointer = 2 * memory.size / 3
        callPointerOrigin = callPointer

        var i = instructionPointer

        for (value in values) {
            memory[i++] = value
        }

        stackPointer = i
        stackPointerOrigin = stackPointer
    }

    private fun fetch() = memory[instructionPointer++]

    private fun pushStack(value: Float) {
        memory[stackPointer++] = value
    }

    private fun popStack() =
        memory[--stackPointer]

    private fun peekStack() = memory[stackPointer]

    private fun pushCall(value: Float) {
        memory[++callPointer] = value
    }

    private fun popCall() =
        memory[callPointer--]

    override fun run(): Float {
        var result = Float.NaN
        var running = true

        while (running) {
            when (val instruction = ASMToken.Instruction.entries[fetch().toInt()]) {
                ASMToken.Instruction.HALT  -> {
                    result = popStack()

                    running = false
                }

                ASMToken.Instruction.PUSH  -> {
                    val value = fetch()

                    Debug.println("PUSH ${value.truncate()}")

                    pushStack(value)
                }

                ASMToken.Instruction.POP   -> {
                    Debug.println("POP")

                    popStack()
                }

                ASMToken.Instruction.DUP   -> {
                    val value = peekStack()

                    Debug.println("DUP ${value.truncate()}")

                    pushStack(value)
                }

                ASMToken.Instruction.ADD   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("ADD ${a.truncate()} ${b.truncate()}")

                    pushStack(a + b)
                }

                ASMToken.Instruction.SUB   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("SUB ${a.truncate()} ${b.truncate()}")

                    pushStack(a - b)
                }

                ASMToken.Instruction.MUL   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("MUL ${a.truncate()} ${b.truncate()}")

                    pushStack(a * b)
                }

                ASMToken.Instruction.DIV   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("DIV ${a.truncate()} ${b.truncate()}")

                    pushStack(a / b)
                }

                ASMToken.Instruction.MOD   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("MOD ${a.truncate()} ${b.truncate()}")

                    pushStack(a % b)
                }

                ASMToken.Instruction.NEG   -> {
                    val value = popStack()

                    Debug.println("NEG ${value.truncate()}")

                    pushStack(-value)
                }

                ASMToken.Instruction.AND   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("AND ${a.truncate()} ${b.truncate()}")

                    pushStack((a.toBool() && b.toBool()).toFloat())
                }

                ASMToken.Instruction.OR    -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("OR ${a.truncate()} ${b.truncate()}")

                    pushStack((a.toBool() || b.toBool()).toFloat())
                }

                ASMToken.Instruction.NOT   -> {
                    val value = popStack()

                    Debug.println("NOT ${value.truncate()}")

                    pushStack((!value.toBool()).toFloat())
                }

                ASMToken.Instruction.EQU   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("EQU ${a.truncate()} ${b.truncate()}")

                    pushStack((a == b).toFloat())
                }

                ASMToken.Instruction.GRT   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("GRT ${a.truncate()} ${b.truncate()}")

                    pushStack((a > b).toFloat())
                }

                ASMToken.Instruction.GEQ   -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("GEQ ${a.truncate()} ${b.truncate()}")

                    pushStack((a >= b).toFloat())
                }

                ASMToken.Instruction.JMP   -> {
                    val address = instructionPointerOrigin + fetch().toInt()

                    instructionPointer = address
                }

                ASMToken.Instruction.JIF   -> {
                    val address = instructionPointerOrigin + fetch().toInt()

                    if (popStack().toBool()) {
                        instructionPointer = address
                    }
                }

                ASMToken.Instruction.LOAD  -> pushStack(memory[fetch().toInt() + variablePointer])

                ASMToken.Instruction.STORE -> memory[fetch().toInt() + variablePointer] = popStack()

                ASMToken.Instruction.CALL  -> {
                    val address = instructionPointer + 1
                    instructionPointer = instructionPointerOrigin + fetch().toInt()

                    pushCall(address.toFloat())
                }

                ASMToken.Instruction.RET   -> {
                    val address = popCall()

                    instructionPointer = address.toInt()
                }

                ASMToken.Instruction.PEEK  -> Debug.println("[TOP_OF_STACK = ${peekStack().truncate()}]")

                else                       -> TODO("Instruction $instruction is not implemented.")
            }

            Debug {
                print("STACK: ")

                for (i in stackPointerOrigin..<stackPointer) {
                    print("${memory[i].truncate()} ")
                }

                println()
            }
        }

        return result
    }
}