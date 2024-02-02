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

        var i = instructionPointer

        for (value in values) {
            memory[i++] = value
        }

        framePointer = i
        framePointerOrigin = framePointer

        i += 10_000

        callPointer = i
        callPointerOrigin = callPointer

        i += 10_000

        stackPointer = i
        stackPointerOrigin = stackPointer
    }

    private fun fetch() = memory[instructionPointer++]

    private fun fetchInt() = fetch().toInt()

    private fun pushStack(value: Float) {
        memory[stackPointer++] = value
    }

    private fun popStack(): Float {
        val address = --stackPointer

        if (address < stackPointerOrigin) error("Stack underflow!")

        return memory[address]
    }

    private fun peekStack() = memory[stackPointer - 1]

    private fun pushFrame(offset: Int) {
        framePointer += offset

        memory[framePointer++] = offset.toFloat()
    }

    private fun popFrame() {
        val offset = memory[--framePointer]

        framePointer -= offset.toInt()

        if (framePointer < framePointerOrigin) error("Frame stack underflow!")
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
            Debug {
                for (i in framePointerOrigin until framePointerOrigin + 20) {
                    print("${memory[i].truncate()} ")
                }

                println()
            }

            when (ASMToken.Instruction.entries[fetchInt()]) {
                ASMToken.Instruction.HALT    -> {
                    result = popStack()

                    Debug.println("HALT #${result.truncate()}")

                    running = false
                }

                ASMToken.Instruction.PUSH    -> {
                    val value = fetch()

                    Debug.println("PUSH #${value.truncate()}")

                    pushStack(value)
                }

                ASMToken.Instruction.POP     -> {
                    val value = popStack()

                    Debug.println("POP <${value.truncate()}>")
                }

                ASMToken.Instruction.DUP     -> {
                    val value = peekStack()

                    Debug.println("DUP <${value.truncate()}>")

                    pushStack(value)
                }

                ASMToken.Instruction.ADD     -> {
                    val b = popStack()
                    val a = popStack()

                    val value = a + b

                    Debug.println("ADD #${a.truncate()} #${b.truncate()} <$value>")

                    pushStack(value)
                }

                ASMToken.Instruction.SUB     -> {
                    val b = popStack()
                    val a = popStack()

                    val value = a - b

                    Debug.println("SUB #${a.truncate()} #${b.truncate()} <$value>")

                    pushStack(value)
                }

                ASMToken.Instruction.MUL     -> {
                    val b = popStack()
                    val a = popStack()

                    val value = a * b

                    Debug.println("MUL #${a.truncate()} #${b.truncate()} <$value>")

                    pushStack(value)
                }

                ASMToken.Instruction.DIV     -> {
                    val b = popStack()
                    val a = popStack()

                    val value = a / b

                    Debug.println("DIV #${a.truncate()} #${b.truncate()} <$value>")

                    pushStack(value)
                }

                ASMToken.Instruction.IDIV    -> {
                    val b = popStack()
                    val a = popStack()

                    val value = (a.toInt() / b.toInt()).toFloat()

                    Debug.println("IDIV #${a.truncate()} #${b.truncate()} <$value>")

                    pushStack(value)
                }

                ASMToken.Instruction.MOD     -> {
                    val b = popStack()
                    val a = popStack()

                    val value = a % b

                    Debug.println("MOD #${a.truncate()} #${b.truncate()} <$value>")

                    pushStack(value)
                }

                ASMToken.Instruction.IMOD    -> {
                    val b = popStack()
                    val a = popStack()

                    val value = (a.toInt() % b.toInt()).toFloat()

                    Debug.println("IMOD #${a.truncate()} #${b.truncate()} <$value>")

                    pushStack(value)
                }

                ASMToken.Instruction.NEG     -> {
                    val a = popStack()

                    val value = -a

                    Debug.println("NEG #${value.truncate()}")

                    pushStack(value)
                }

                ASMToken.Instruction.AND     -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("AND #${a.truncate()} #${b.truncate()}")

                    pushStack((a.toBool() && b.toBool()).toFloat())
                }

                ASMToken.Instruction.OR      -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("OR #${a.truncate()} #${b.truncate()}")

                    pushStack((a.toBool() || b.toBool()).toFloat())
                }

                ASMToken.Instruction.NOT     -> {
                    val value = popStack()

                    Debug.println("NOT #${value.truncate()}")

                    pushStack((!value.toBool()).toFloat())
                }

                ASMToken.Instruction.EQU     -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("EQU #${a.truncate()} #${b.truncate()}")

                    pushStack((a == b).toFloat())
                }

                ASMToken.Instruction.GRT     -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("GRT #${a.truncate()} #${b.truncate()}")

                    pushStack((a > b).toFloat())
                }

                ASMToken.Instruction.GEQ     -> {
                    val b = popStack()
                    val a = popStack()

                    Debug.println("GEQ #${a.truncate()} #${b.truncate()}")

                    pushStack((a >= b).toFloat())
                }

                ASMToken.Instruction.JMP     -> {
                    val address = instructionPointerOrigin + fetchInt()

                    Debug.println("JMP @${address.toAddress()}")

                    instructionPointer = address
                }

                ASMToken.Instruction.JIF     -> {
                    val address = instructionPointerOrigin + fetchInt()

                    Debug.println("JIF @${address.toAddress()}")

                    if (popStack().toBool()) {
                        instructionPointer = address
                    }
                }

                ASMToken.Instruction.LOAD    -> {
                    val address = fetchInt() + framePointer
                    val value = memory[address]

                    Debug.println("LOAD @${address.toAddress()} <${value.truncate()}>")

                    pushStack(value)
                }

                ASMToken.Instruction.ALOAD   -> {
                    val address = fetchInt() + framePointer
                    val size = memory[address]

                    val elements = FloatArray(size.toInt()) { memory[address + 1 + it] }

                    Debug.println("ALOAD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

                    for (element in elements.reversed()) {
                        pushStack(element)
                    }

                    pushStack(size)
                }

                ASMToken.Instruction.ILOAD   -> {
                    var address = fetchInt() + framePointer
                    val indexCount = fetchInt()

                    var indexOffset = 0

                    for (i in 0..<indexCount) {
                        val subSize = memory[address + indexOffset + 1].toInt()

                        val index = popStack().toInt()

                        indexOffset = (indexOffset + 1) + (index * subSize)
                    }

                    address += indexOffset

                    val value = memory[address]

                    Debug.println("ILOAD @${address.toAddress()} #$indexCount <${value.truncate()}>")

                    pushStack(value)
                }

                ASMToken.Instruction.IALOAD  -> {
                    var address = fetchInt() + framePointer
                    val indexCount = fetchInt()

                    var indexOffset = 0

                    for (i in 0..<indexCount - 1) {
                        val subSize = memory[address + indexOffset + 1].toInt()

                        val index = popStack().toInt()

                        indexOffset = (indexOffset + 1) + (index * subSize)
                    }

                    address += indexOffset
                    val size = memory[address]

                    val elements = FloatArray(size.toInt()) { memory[address + 1 + it] }

                    Debug.println("IALOAD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

                    for (element in elements.reversed()) {
                        pushStack(element)
                    }

                    pushStack(size)
                }

                ASMToken.Instruction.LOADG   -> {
                    val address = fetchInt() + framePointerOrigin
                    val value = memory[address]

                    Debug.println("LOADG @${address.toAddress()} <${value.truncate()}>")

                    pushStack(value)
                }

                ASMToken.Instruction.ALOADG  -> {
                    val address = fetchInt() + framePointerOrigin
                    val size = memory[address]

                    val elements = FloatArray(size.toInt()) { memory[address + 1 + it] }

                    Debug.println("ALOADG @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

                    for (element in elements.reversed()) {
                        pushStack(element)
                    }

                    pushStack(size)
                }

                ASMToken.Instruction.ILOADG  -> {
                    var address = fetchInt() + framePointerOrigin
                    val indexCount = fetchInt()

                    var indexOffset = 0

                    for (i in 0..<indexCount - 1) {
                        val subSize = memory[address + indexOffset + 1].toInt()

                        val index = popStack().toInt()

                        indexOffset = (indexOffset + 1) + (index * (subSize + 1))
                    }

                    val index = popStack().toInt()

                    indexOffset = (indexOffset + 1) + index

                    address += indexOffset

                    val value = memory[address]

                    Debug.println("ILOADG @${address.toAddress()} #$indexCount <${value.truncate()}>")

                    pushStack(value)
                }

                ASMToken.Instruction.IALOADG -> {
                    var address = fetchInt() + framePointerOrigin
                    val indexCount = fetchInt()

                    var indexOffset = 0

                    for (i in 0..<indexCount - 1) {
                        val subSize = memory[address + indexOffset + 1].toInt()

                        val index = popStack().toInt()

                        indexOffset = (indexOffset + 1) + (index * subSize)
                    }

                    address += indexOffset
                    val size = memory[address]

                    val elements = FloatArray(size.toInt()) { memory[address + 1 + it] }

                    Debug.println("IALOADG @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

                    for (element in elements.reversed()) {
                        pushStack(element)
                    }

                    pushStack(size)
                }

                ASMToken.Instruction.STORE   -> {
                    val address = fetchInt() + framePointer
                    val value = popStack()

                    Debug.println("STORE @${address.toAddress()} <${value.truncate()}>")

                    memory[address] = value
                }

                ASMToken.Instruction.ASTORE  -> {
                    val address = fetchInt() + framePointer
                    val size = popStack()

                    val elements = FloatArray(size.toInt()) {
                        popStack()
                    }

                    Debug.println("ASTORE @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

                    memory[address] = size

                    for (offset in elements.indices) {
                        memory[address + offset + 1] = elements[offset]
                    }
                }

                ASMToken.Instruction.ISTORE  -> {
                    var address = fetchInt() + framePointer
                    val indexCount = fetchInt()

                    var indexOffset = 0

                    for (i in 0..<indexCount - 1) {
                        val subSize = memory[address + indexOffset + 1].toInt()

                        val index = popStack().toInt()

                        indexOffset = (indexOffset + 1) + (index * subSize)
                    }

                    val index = popStack().toInt()

                    indexOffset += index + 1

                    address += indexOffset

                    val value = popStack()

                    Debug.println("ISTORE @${address.toAddress()} #$indexCount <${value.truncate()}>")

                    memory[address] = value
                }

                ASMToken.Instruction.IASTORE -> TODO("IASTORE")

                ASMToken.Instruction.CALL    -> {
                    val address = instructionPointer + 1
                    instructionPointer = instructionPointerOrigin + fetchInt()

                    Debug.println("CALL @${instructionPointer.toAddress()}")

                    pushCall(address.toFloat())
                }

                ASMToken.Instruction.RET     -> {
                    val address = popCall()

                    Debug.println("RET @${address.toAddress()}")

                    if (address < 0) {
                        result = popStack()

                        running = false

                        continue
                    }

                    instructionPointer = address

                    popFrame()
                }

                ASMToken.Instruction.FRAME   -> {
                    val value = fetchInt()

                    Debug.println("FRAME $$value")

                    pushFrame(value)
                }

                ASMToken.Instruction.SYS     -> {
                    val id = fetchInt()

                    val function = SystemFunctions[id]

                    val args = mutableListOf<Float>()

                    repeat(function.signature.arity) {
                        args.add(popStack())
                    }

                    Debug.println("SYS #$id <${args.joinToString()}>")

                    pushStack(function(args))
                }
            }

            Debug {
                print("\t\t\t\t\t\t\t\tSTACK:")

                for (i in stackPointerOrigin..<stackPointer) {
                    print(" ${memory[i].truncate()}")
                }

                println()
            }
        }

        return result
    }
}