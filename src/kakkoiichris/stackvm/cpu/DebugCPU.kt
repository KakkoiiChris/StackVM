package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.util.bool
import kakkoiichris.stackvm.util.float
import kakkoiichris.stackvm.util.truncate
import kotlin.math.absoluteValue

object DebugCPU : CPU() {
    override fun initialize(instructions: FloatArray) {
        memory = FloatArray(config.memorySize)

        running = true
        result = Float.NaN

        instructionPointer = 11
        instructionPointerOrigin = instructionPointer

        var address = instructionPointer

        for (value in instructions) {
            memory[address++] = value
        }

        callPointer = address
        callPointerOrigin = callPointer

        address += config.maxCalls

        stackPointer = address
        stackPointerOrigin = stackPointer

        address += config.maxStack

        framePointer = address
        framePointerOrigin = framePointer

        pushStack(0F)
    }

    private fun Int.toAddress() =
        if (this < 0)
            "-0x${absoluteValue.toString(16)}"
        else
            "0x${toString(16)}"

    override fun run(): Float {
        while (running) {
            for (i in framePointerOrigin until framePointerOrigin + 20) {
                print("${memory[i].truncate()} ")
            }

            println()

            decode()

            print("\t\t\t\t\t\t\t\tSTACK:")

            for (i in stackPointerOrigin..<stackPointer) {
                print(" ${memory[i].truncate()}")
            }

            println()
        }

        return result
    }

    override fun halt() {
        result = popStack()

        println("HALT #${result.truncate()}")

        running = false
    }

    override fun push() {
        val value = fetch()

        println("PUSH #${value.truncate()}")

        pushStack(value)
    }

    override fun pop() {
        val value = popStack()

        println("POP <${value.truncate()}>")
    }

    override fun dup() {
        val value = peekStack()

        println("DUP <${value.truncate()}>")

        pushStack(value)
    }

    override fun add() {
        val b = popStack()
        val a = popStack()

        val value = a + b

        println("ADD #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    override fun sub() {
        val b = popStack()
        val a = popStack()

        val value = a - b

        println("SUB #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    override fun mul() {
        val b = popStack()
        val a = popStack()

        val value = a * b

        println("MUL #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    override fun div() {
        val b = popStack()
        val a = popStack()

        val value = a / b

        println("DIV #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    override fun idiv() {
        val b = popStack()
        val a = popStack()

        val value = (a.toInt() / b.toInt()).toFloat()

        println("IDIV #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    override fun mod() {
        val b = popStack()
        val a = popStack()

        val value = a % b

        println("MOD #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    override fun imod() {
        val b = popStack()
        val a = popStack()

        val value = (a.toInt() % b.toInt()).toFloat()

        println("IMOD #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    override fun neg() {
        val a = popStack()

        val value = -a

        println("NEG #${value.truncate()}")

        pushStack(value)
    }

    override fun and() {
        val b = popStack()
        val a = popStack()

        println("AND #${a.truncate()} #${b.truncate()}")

        pushStack((a.bool && b.bool).float)
    }

    override fun or() {
        val b = popStack()
        val a = popStack()

        println("OR #${a.truncate()} #${b.truncate()}")

        pushStack((a.bool || b.bool).float)
    }

    override fun not() {
        val value = popStack()

        println("NOT #${value.truncate()}")

        pushStack((!value.bool).float)
    }

    override fun equ() {
        val b = popStack()
        val a = popStack()

        println("EQU #${a.truncate()} #${b.truncate()}")

        pushStack((a == b).float)
    }

    override fun grt() {
        val b = popStack()
        val a = popStack()

        println("GRT #${a.truncate()} #${b.truncate()}")

        pushStack((a > b).float)
    }

    override fun geq() {
        val b = popStack()
        val a = popStack()

        println("GEQ #${a.truncate()} #${b.truncate()}")

        pushStack((a >= b).float)
    }

    override fun jmp() {
        val address = instructionPointerOrigin + fetchInt()

        println("JMP @${address.toAddress()}")

        instructionPointer = address
    }

    override fun jif() {
        val address = instructionPointerOrigin + fetchInt()

        println("JIF @${address.toAddress()}")

        if (popStackBool()) {
            instructionPointer = address
        }
    }

    override fun global() {
        global = true

        println("GLOBAL")
    }

    override fun load() {
        val address = fetchInt() + getLoadOffset()
        val value = memory[address]

        println("LOAD @${address.toAddress()} <${value.truncate()}>")

        pushStack(value)
    }

    override fun aload() {
        val address = fetchInt() + getLoadOffset()
        val size = memory[address]

        val elements = FloatArray(size.toInt()) { memory[address + 1 + it] }

        println("ALOAD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }

        pushStack(size)
    }

    override fun iload() {
        var address = fetchInt() + getLoadOffset()
        val indexCount = fetchInt()

        var indexOffset = 0

        for (i in 0..<indexCount - 1) {
            val subSize = memory[address + indexOffset + 1].toInt()

            val index = popStackInt()

            indexOffset = (indexOffset + 1) + (index * subSize)
        }

        indexOffset++

        val index = popStackInt()

        indexOffset += index

        address += indexOffset

        val value = memory[address]

        println("ILOAD @${address.toAddress()} #$indexCount <${value.truncate()}>")

        pushStack(value)
    }

    override fun iaload() {
        var address = fetchInt() + getLoadOffset()
        val indexCount = fetchInt()

        var indexOffset = 0

        for (i in 0..<indexCount - 1) {
            val subSize = memory[address + indexOffset + 1].toInt()

            val index = popStackInt()

            indexOffset = (indexOffset + 1) + (index * subSize)
        }

        address += indexOffset
        val size = memory[address]

        val elements = FloatArray(size.toInt()) { memory[address + 1 + it] }

        println("IALOAD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }

        pushStack(size)
    }

    override fun store() {
        val address = fetchInt() + framePointer
        val value = popStack()

        println("STORE @${address.toAddress()} <${value.truncate()}>")

        memory[address] = value
    }

    override fun astore() {
        val address = fetchInt() + framePointer
        val size = popStack()

        val elements = FloatArray(size.toInt()) {
            popStack()
        }

        println("ASTORE @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        memory[address] = size

        for (offset in elements.indices) {
            memory[address + offset + 1] = elements[offset]
        }
    }

    override fun istore() {
        var address = fetchInt() + framePointer
        val indexCount = fetchInt()

        var indexOffset = 0

        for (i in 0..<indexCount - 1) {
            val subSize = memory[address + indexOffset + 1].toInt()

            val index = popStackInt()

            indexOffset = (indexOffset + 1) + (index * subSize)
        }

        val index = popStackInt()

        indexOffset += index + 1

        address += indexOffset

        val value = popStack()

        println("ISTORE @${address.toAddress()} #$indexCount <${value.truncate()}>")

        memory[address] = value
    }

    override fun iastore() {
        TODO("IASTORE")
    }

    override fun size() {
        val address = fetchInt() + getLoadOffset()

        val totalSize = memory[address].toInt()

        pushStack(totalSize.toFloat())
    }

    override fun call() {
        val address = instructionPointer + 1
        instructionPointer = instructionPointerOrigin + fetchInt()

        println("CALL @${instructionPointer.toAddress()}")

        pushCall(address.toFloat())
    }

    override fun ret() {
        val address = popCall()

        println("RET @${address.toAddress()}")

        if (address < 0) {
            result = popStack()

            running = false

            return
        }

        instructionPointer = address

        popFrame()
    }

    override fun frame() {
        val value = fetchInt()

        println("FRAME $$value")

        pushFrame(value)
    }

    override fun sys() {
        val id = fetchInt()

        val function = StandardLibrary[id]

        val args = mutableListOf<Float>()

        repeat(function.signature.arity) {
            args.add(popStack())
        }

        println("SYS #$id <${args.joinToString()}>")

        val result = function(args)

        for (value in result.reversed()) {
            pushStack(value)
        }
    }
}