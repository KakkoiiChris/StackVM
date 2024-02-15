package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.util.bool
import kakkoiichris.stackvm.util.float
import kakkoiichris.stackvm.util.toAddress
import kakkoiichris.stackvm.util.truncate

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

    override fun run(): Float {
        while (running) {
            showMemory()

            decode()

            showStack()
        }

        return result
    }

    private fun showMemory() {
        for (i in framePointerOrigin until framePointerOrigin + 30) {
            print("${memory[i].truncate()} ")
        }

        println()
    }

    private fun showStack() {
        print("\t\t\t\t\t\t\t\tSTACK:")

        for (i in stackPointerOrigin..<stackPointer) {
            print(" ${memory[i].truncate()}")
        }

        println()
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

        val elements = MutableList(size.toInt()) { memory[address + 1 + it] }

        elements.add(0, size)

        println("ALOAD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }
    }

    override fun iload() {
        var address = fetchInt() + getLoadOffset()
        val indexCount = fetchInt()

        val indices = List(indexCount) { popStackInt() }

        for (index in indices.dropLast(1)) {
            address++

            val subSize = memory[address].toInt()

            address += index * (subSize + 1)
        }

        address += indices.last() + 1

        val value = memory[address]

        println("ILOAD @${address.toAddress()} #$indexCount <[${indices.joinToString(separator = "][")}], ${value.truncate()}>")

        pushStack(value)
    }

    override fun iaload() {
        var address = fetchInt() + getLoadOffset()
        val indexCount = fetchInt()

        val indices = List(indexCount) { popStackInt() }

        for (index in indices.dropLast(1)) {
            address++

            val subSize = memory[address].toInt()

            address += index * (subSize + 1)
        }

        address += indices.last()

        val size = memory[address]

        val elements = FloatArray(size.toInt() + 1) { memory[address + it] }

        println("IALOAD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }
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

        val elements = MutableList(size.toInt()) {
            popStack()
        }

        elements.add(0, size)

        println("ASTORE @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (offset in elements.indices) {
            memory[address + offset] = elements[offset]
        }
    }

    override fun istore() {
        var address = fetchInt() + framePointer
        val indexCount = fetchInt()

        for (i in 0 until indexCount - 1) {
            address++

            val subSize = memory[address].toInt()

            address += popStackInt() * (subSize + 1)
        }

        address += popStackInt() + 1

        val value = popStack()

        println("ISTORE @${address.toAddress()} #$indexCount <${value.truncate()}>")

        memory[address] = value
    }

    override fun iastore() {
        var address = fetchInt() + framePointer
        val indexCount = fetchInt()

        for (i in 0 until indexCount - 1) {
            address++

            val subSize = memory[address].toInt()

            address += popStackInt() * (subSize + 1)
        }

        address += popStackInt() + 1

        val size = popStack()

        val elements = MutableList(size.toInt()) {
            popStack()
        }

        elements.add(0, size)

        println("IASTORE @${address.toAddress()} #$indexCount [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (offset in elements.indices) {
            memory[address + offset] = elements[offset]
        }
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

        val result = function(this, args)

        for (value in result.reversed()) {
            pushStack(value)
        }
    }
}