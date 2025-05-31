/*   ______  ____   ____  ____    ____  _____
 * .' ____ \|_  _| |_  _||_   \  /   _||_   _|
 * | (___ \_| \ \   / /    |   \/   |    | |
 *  _.____`.   \ \ / /     | |\  /| |    | |   _
 * | \____) |   \ ' /     _| |_\/_| |_  _| |__/ |
 *  \______.'    \_/     |_____||_____||________|
 *
 *         Stack Virtual Machine Language
 *     Copyright (C) 2024 Christian Alexander
 */
package kakkoiichris.svml.cpu

import kakkoiichris.svml.linker.Linker
import kakkoiichris.svml.util.bool
import kakkoiichris.svml.util.float

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

    override fun inc() {
        val a = popStack()

        pushStack(a + 1)
    }

    override fun dec() {
        val a = popStack()

        pushStack(a - 1)
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

    override fun lod() {
        val address = getAddress()

        pushStack(memory[address])
    }

    override fun alod() {
        val address = getAddress()
        val size = memory[address].toInt()

        for (i in size downTo 0) {
            pushStack(memory[address + i])
        }
    }

    override fun hlod() {
        val address = getHeapAddress()

        pushStack(memory[address])
    }

    override fun halod() {
        val address = getHeapAddress()
        val size = memory[address].toInt()

        for (i in size downTo 0) {
            pushStack(memory[address + i])
        }
    }

    override fun sto() {
        val address = getAddress()

        memory[address] = popStack()
    }

    override fun asto() {
        val address = getAddress()
        val size = popStack()

        memory[address] = size

        for (i in 1..size.toInt()) {
            memory[address + i] = popStack()
        }
    }

    override fun hsto() {
        val address = getHeapAddress()

        memory[address] = popStack()
    }

    override fun hasto() {
        val address = getHeapAddress()
        val size = popStack()

        memory[address] = size

        for (i in 1..size.toInt()) {
            memory[address + i] = popStack()
        }
    }

    override fun size() {
        val address = getAddress()

        val totalSize = memory[address].toInt()

        pushStack(totalSize.toDouble())
    }

    override fun asize() {
        val address = getAddress()

        val size = memory[address].toInt()
        val subSize = (memory[address + 1] + 1).toInt()

        val totalSize = size / subSize

        pushStack(totalSize.toDouble())
    }

    override fun hsize() {
        val address = getHeapAddress()

        val totalSize = memory[address].toInt()

        pushStack(totalSize.toDouble())
    }

    override fun hasize() {
        val address = getHeapAddress()

        val size = memory[address].toInt()
        val subSize = (memory[address + 1] + 1).toInt()

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
        val function = Linker[fetchInt()]!!

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