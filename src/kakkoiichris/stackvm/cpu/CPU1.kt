package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.asm.ASMToken
import kakkoiichris.stackvm.util.toAddress
import kakkoiichris.stackvm.util.toBool
import kakkoiichris.stackvm.util.toFloat
import kakkoiichris.stackvm.util.truncate

object CPU1 : CPU() {
    private const val IPO_ADR = 0
    private const val IPA_ADR = 1
    private const val SPO_ADR = 2
    private const val SPA_ADR = 3
    private const val FPO_ADR = 4
    private const val FPA_ADR = 5
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

    private var framePointerOrigin: Int
        get() = memory[FPO_ADR].toInt()
        set(value) {
            memory[FPO_ADR] = value.toFloat()
        }

    private var framePointer: Int
        get() = memory[FPA_ADR].toInt()
        set(value) {
            memory[FPA_ADR] = value.toFloat()
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

    fun load(tokenizer: Iterator<ASMToken>): Unit = load(
        tokenizer
            .asSequence()
            .toList()
            .map { it.value }
            .toFloatArray()
    )

    override fun load(values: FloatArray) {
        memory.fill(0F)

        instructionPointer = 8
        instructionPointerOrigin = instructionPointer

        framePointer = memory.size / 3
        framePointerOrigin = framePointer

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

    private fun fetchInt() = fetch().toInt()

    private fun pushStack(value: Float) {
        memory[stackPointer++] = value
    }

    private fun popStack() =
        memory[--stackPointer]

    private fun peekStack() = memory[stackPointer - 1]

    private fun pushFrame(value: Float) {
        memory[++framePointer] = value
    }

    private fun popFrame(): Int {
        if (framePointer > framePointerOrigin) {
            return memory[framePointer--].toInt()
        }

        return -1
    }

    private fun pushCall(value: Float) {
        memory[++callPointer] = value
    }

    private fun popCall(): Int {
        if (callPointer > callPointerOrigin) {
            return memory[callPointer--].toInt()
        }

        return -1
    }

    override fun run(): Float {
        var result = Float.NaN
        var running = true

        while (running) {
            when (ASMToken.Instruction.entries[fetchInt()]) {
                ASMToken.Instruction.HALT -> {
                    result = popStack()

                    Debug.println("HALT #${result.truncate()}")

                    running = false
                }

                ASMToken.Instruction.PUSH -> {
                    val value = fetch()

                    Debug.println("PUSH #${value.truncate()}")

                    pushStack(value)
                }

                ASMToken.Instruction.POP -> {
                    val value = popStack()

                    Debug.println("POP #${value.truncate()}")
                }

                ASMToken.Instruction.DUP -> {
                    val value = peekStack()

                    Debug.println("DUP #${value.truncate()}")

                    pushStack(value)
                }

                ASMToken.Instruction.ADD -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("ADD #${a.truncate()} #${b.truncate()}")

                    pushStack(a + b)
                }

                ASMToken.Instruction.SUB -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("SUB #${a.truncate()} #${b.truncate()}")

                    pushStack(a - b)
                }

                ASMToken.Instruction.MUL -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("MUL #${a.truncate()} #${b.truncate()}")

                    pushStack(a * b)
                }

                ASMToken.Instruction.DIV -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("DIV #${a.truncate()} #${b.truncate()}")

                    pushStack(a / b)
                }

                ASMToken.Instruction.MOD -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("MOD #${a.truncate()} #${b.truncate()}")

                    pushStack(a % b)
                }

                ASMToken.Instruction.NEG -> {
                    val value = popStack()

                    Debug.println("NEG #${value.truncate()}")

                    pushStack(-value)
                }

                ASMToken.Instruction.AND -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("AND #${a.truncate()} #${b.truncate()}")

                    pushStack((a.toBool() && b.toBool()).toFloat())
                }

                ASMToken.Instruction.OR -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("OR #${a.truncate()} #${b.truncate()}")

                    pushStack((a.toBool() || b.toBool()).toFloat())
                }

                ASMToken.Instruction.NOT -> {
                    val value = popStack()

                    Debug.println("NOT #${value.truncate()}")

                    pushStack((!value.toBool()).toFloat())
                }

                ASMToken.Instruction.EQU -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("EQU #${a.truncate()} #${b.truncate()}")

                    pushStack((a == b).toFloat())
                }

                ASMToken.Instruction.GRT -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("GRT #${a.truncate()} #${b.truncate()}")

                    pushStack((a > b).toFloat())
                }

                ASMToken.Instruction.GEQ -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("GEQ #${a.truncate()} #${b.truncate()}")

                    pushStack((a >= b).toFloat())
                }

                ASMToken.Instruction.JMP -> {
                    val address = instructionPointerOrigin + fetchInt()

                    Debug.println("JMP @${address.toAddress()}")

                    instructionPointer = address
                }

                ASMToken.Instruction.JIF -> {
                    val address = instructionPointerOrigin + fetchInt()

                    if (popStack().toBool()) {
                        Debug.println("JIF @${address.toAddress()}")

                        instructionPointer = address
                    }
                }

                ASMToken.Instruction.LOAD -> {
                    val address = fetchInt() + framePointer
                    val value = memory[address]

                    Debug.println("LOAD @${address.toAddress()} #${value.truncate()}")

                    pushStack(value)
                }

                ASMToken.Instruction.LOADG -> {
                    val address = fetchInt() + framePointerOrigin
                    val value = memory[address]

                    Debug.println("LOADG @${address.toAddress()} #${value.truncate()}")

                    pushStack(value)
                }

                ASMToken.Instruction.STORE -> {
                    val address = fetchInt() + framePointer
                    val value = popStack()

                    Debug.println("STORE @${address.toAddress()} #${value.truncate()}")

                    memory[address] = value
                }

                ASMToken.Instruction.CALL -> {
                    val address = instructionPointer + 1
                    instructionPointer = instructionPointerOrigin + fetchInt()

                    Debug.println("CALL @${instructionPointer.toAddress()}")

                    pushCall(address.toFloat())
                }

                ASMToken.Instruction.RET -> {
                    val address = popCall()

                    Debug.println("RET @${address.toAddress()}")

                    instructionPointer = if (address < 0) {
                        stackPointerOrigin - 1
                    }
                    else {
                        address
                    }

                    popFrame()
                }

                ASMToken.Instruction.FRAME -> {
                    val value = fetch()

                    Debug.println("FRAME $$value")

                    pushFrame(value)
                }

                ASMToken.Instruction.SYS -> {
                    val id = fetchInt()

                    val function = SystemFunctions[id]

                    val args = mutableListOf<Float>()

                    repeat(function.arity) {
                        args.add(popStack())
                    }

                    Debug.println("SYS #$id (${args.joinToString()})")

                    pushStack(function(args))
                }
            }

            Debug {
                print("\tSTACK:")

                for (i in stackPointerOrigin..<stackPointer) {
                    print(" ${memory[i].truncate()}")
                }

                println()
            }
        }

        return result
    }
}