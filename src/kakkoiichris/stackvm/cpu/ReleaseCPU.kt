package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.util.bool
import kakkoiichris.stackvm.util.float

object ReleaseCPU : CPU() {
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
            decode()
        }

        return result
    }

    override fun halt() {
        result = popStack()

        running = false
    }

    override fun push() {
        pushStack(fetch())
    }

    override fun pop() {
        popStack()
    }

    override fun dup() {
        pushStack(peekStack())
    }

    override fun add() {
        val b = popStack()
        val a = popStack()

        pushStack(a + b)
    }

    override fun sub() {
        val b = popStack()
        val a = popStack()

        pushStack(a - b)
    }

    override fun mul() {
        val b = popStack()
        val a = popStack()

        pushStack(a * b)
    }

    override fun div() {
        val b = popStack()
        val a = popStack()

        pushStack(a / b)
    }

    override fun idiv() {
        val b = popStack()
        val a = popStack()

        pushStack((a.toInt() / b.toInt()).toFloat())
    }

    override fun mod() {
        val b = popStack()
        val a = popStack()

        pushStack(a % b)
    }

    override fun imod() {
        val b = popStack()
        val a = popStack()

        pushStack((a.toInt() % b.toInt()).toFloat())
    }

    override fun neg() {
        val a = popStack()

        pushStack(-a)
    }

    override fun and() {
        val b = popStack()
        val a = popStack()

        pushStack((a.bool && b.bool).float)
    }

    override fun or() {
        val b = popStack()
        val a = popStack()

        pushStack((a.bool || b.bool).float)
    }

    override fun not() {
        val value = popStack()

        pushStack((!value.bool).float)
    }

    override fun equ() {
        val b = popStack()
        val a = popStack()

        pushStack((a == b).float)
    }

    override fun grt() {
        val b = popStack()
        val a = popStack()

        pushStack((a > b).float)
    }

    override fun geq() {
        val b = popStack()
        val a = popStack()

        pushStack((a >= b).float)
    }

    override fun jmp() {
        val address = instructionPointerOrigin + fetchInt()

        instructionPointer = address
    }

    override fun jif() {
        val address = instructionPointerOrigin + fetchInt()

        if (popStackBool()) {
            instructionPointer = address
        }
    }

    override fun global() {
        global = true
    }

    override fun load() {
        val address = fetchInt() + getLoadOffset()

        pushStack(memory[address])
    }

    override fun aload() {
        val address = fetchInt() + getLoadOffset()
        val size = memory[address].toInt()

        for (i in size downTo 0) {
            pushStack(memory[address + i])
        }
    }

    override fun iload() {
        var address = fetchInt() + getLoadOffset()
        val indexCount = fetchInt()

        for (i in 0 until indexCount - 1) {
            address++

            val subSize = memory[address].toInt()

            address += popStackInt() * (subSize + 1)
        }

        address += popStackInt() + 1

        pushStack(memory[address])
    }

    override fun iaload() {
        var address = fetchInt() + getLoadOffset()
        val indexCount = fetchInt()

        for (i in 0 until indexCount - 1) {
            address++

            val subSize = memory[address].toInt()

            address += popStackInt() * (subSize + 1)
        }

        address += popStackInt()

        val size = memory[address].toInt()

        for (i in size downTo 0) {
            pushStack(memory[address + i])
        }
    }

    override fun store() {
        val address = fetchInt() + framePointer

        memory[address] = popStack()
    }

    override fun astore() {
        val address = fetchInt() + framePointer
        val size = popStack()

        memory[address] = size

        for (i in 1..size.toInt()) {
            memory[address + i] = popStack()
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

        memory[address] = popStack()
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

        memory[address] = size

        for (i in 1..size.toInt()) {
            memory[address + i] = popStack()
        }
    }

    override fun size() {
        val address = fetchInt() + getLoadOffset()

        val totalSize = memory[address].toInt()

        pushStack(totalSize.toFloat())
    }

    override fun call() {
        pushCall(instructionPointer + 1F)

        instructionPointer = instructionPointerOrigin + fetchInt()
    }

    override fun ret() {
        val address = popCall()

        if (address < 0) {
            result = popStack()

            running = false

            return
        }

        instructionPointer = address

        popFrame()
    }

    override fun frame() {
        pushFrame(fetchInt())
    }

    override fun sys() {
        val function = StandardLibrary[fetchInt()]

        val args = mutableListOf<Float>()

        repeat(function.signature.arity) {
            args.add(popStack())
        }

        val result = function(args)

        for (value in result.reversed()) {
            pushStack(value)
        }
    }
}