package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.linker.Linker
import kakkoiichris.stackvm.util.bool
import kakkoiichris.stackvm.util.float

object ReleaseCPU : CPU() {
    override fun run(): Double {
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

        pushStack((a.toInt() / b.toInt()).toDouble())
    }

    override fun mod() {
        val b = popStack()
        val a = popStack()

        pushStack(a % b)
    }

    override fun imod() {
        val b = popStack()
        val a = popStack()

        pushStack((a.toInt() % b.toInt()).toDouble())
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

    override fun glob() {
        global = true
    }

    override fun heap() {
        heap = true
    }

    override fun lod() {
        val address = getLoadAddress()

        pushStack(memory[address])
    }

    override fun alod() {
        val address = getLoadAddress()
        val size = memory[address].toInt()

        for (i in size downTo 0) {
            pushStack(memory[address + i])
        }
    }

    override fun ilod() {
        var address = getLoadAddress()
        val indexCount = fetchInt()

        for (i in 0 until indexCount - 1) {
            address++

            val subSize = memory[address].toInt()

            address += popStackInt() * (subSize + 1)
        }

        address += popStackInt() + 1

        pushStack(memory[address])
    }

    override fun ialod() {
        var address = getLoadAddress()
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

    override fun sto() {
        val address = getStoreAddress()

        memory[address] = popStack()
    }

    override fun asto() {
        val address = getStoreAddress()
        val size = popStack()

        memory[address] = size

        for (i in 1..size.toInt()) {
            memory[address + i] = popStack()
        }
    }

    override fun isto() {
        var address = getStoreAddress()
        val indexCount = fetchInt()

        for (i in 0 until indexCount - 1) {
            address++

            val subSize = memory[address].toInt()

            address += popStackInt() * (subSize + 1)
        }

        address += popStackInt() + 1

        memory[address] = popStack()
    }

    override fun iasto() {
        var address = getStoreAddress()
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
        val address = getLoadAddress()

        val totalSize = memory[address].toInt()

        pushStack(totalSize.toDouble())
    }

    override fun asize() {
        val address = getLoadAddress()

        val size = memory[address].toInt()
        val subSize = (memory[address + 1] + 1).toInt()

        val totalSize = size / subSize

        pushStack(totalSize.toDouble())
    }

    override fun isize() {
        var address = getLoadAddress()
        val indexCount = fetchInt()

        for (i in 0 until indexCount - 1) {
            val subSize = memory[++address].toInt()

            address += popStackInt() * (subSize + 1)
        }

        address += popStackInt() + 1

        val totalSize = memory[address].toInt()

        pushStack(totalSize.toDouble())
    }

    override fun iasize() {
        var address = getLoadAddress()
        val indexCount = fetchInt()
        //showMemory(address)

        for (i in 0 until indexCount - 1) {
            address++
            //showMemory(address)

            val subSize = memory[address].toInt()

            address += popStackInt() * (subSize + 1)
            //showMemory(address)
        }

        var subSize = memory[++address].toInt()

        address += popStackInt() * (subSize + 1)
        //showMemory(address)

        val size = memory[address].toInt()
        subSize = (memory[address + 1] + 1).toInt()
        val totalSize = size / subSize

        pushStack(totalSize.toDouble())
    }

    override fun alloc() {
        allocateMemory(fetchInt(), peekStackInt())
    }

    override fun realloc() {
        reallocateMemory(fetchInt(), peekStackInt())
    }

    override fun free() {
        freeMemory(fetchInt())
    }

    override fun call() {
        pushCall(instructionPointer + 1.0)

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

    override fun arg() {
        pushCall(stackPointer.toDouble())
    }

    override fun sys() {
        val function = Linker[fetchInt()]

        val arguments = mutableListOf<Double>()

        val argPointer = popCall()

        while (stackPointer > argPointer) {
            arguments.add(popStack())
        }

        val result = function(this, arguments)

        for (value in result.reversed()) {
            pushStack(value)
        }
    }
}