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
import kakkoiichris.svml.util.toAddress
import kakkoiichris.svml.util.truncate

object DebugCPU : CPU() {
    override fun run(): Double {
        showMemory()

        while (running) {
            decode()

            showMemory()
        }

        return result
    }

    private fun showMemory() {
        print("MEMORY : ")

        for (i in 0..<30) {
            print(" ${memory[framePointerOrigin + i].truncate()}")
        }

        print("\nTABLE  : ")

        for (i in 0..<30) {
            print(" ${memory[tablePointerOrigin + i].truncate()}")
        }

        print("\nHEAP   : ")

        for (i in 0..<30) {
            print(" ${memory[heapPointerOrigin + i].truncate()}")
        }

        print("\nSTACK  : ")

        for (i in stackPointerOrigin..<stackPointer) {
            print(" ${memory[i].truncate()}")
        }

        println("\n")
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

        val value = (a.toInt() / b.toInt()).toDouble()

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

        val value = (a.toInt() % b.toInt()).toDouble()

        println("IMOD #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    override fun neg() {
        val a = popStack()

        val value = -a

        println("NEG #${value.truncate()}")

        pushStack(value)
    }

    override fun inc() {
        val a = popStack()

        val value = a + 1

        println("INC #${value.truncate()}")

        pushStack(value)
    }

    override fun dec() {
        val a = popStack()

        val value = a - 1

        println("DEC #${value.truncate()}")

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
        val index = fetchInt()

        val address = instructionPointerOrigin + index

        println("JMP @${index.toAddress()}")

        instructionPointer = address
    }

    override fun jif() {
        val index = fetchInt()

        val address = instructionPointerOrigin + index

        println("JIF @${index.toAddress()}")

        if (popStackBool()) {
            instructionPointer = address
        }
    }

    override fun glob() {
        global = true

        println("GLOB")
    }

    override fun lod() {
        val address = getAddress()
        val value = memory[address]

        println("LOD @${address.toAddress()} <${value.truncate()}>")

        pushStack(value)
    }

    override fun alod() {
        val address = getAddress()
        val size = memory[address].toInt()

        val elements = MutableList(size + 1) { memory[address + it] }

        println("ALOD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }
    }

    override fun hlod() {
        val address = getHeapAddress()
        val value = memory[address]

        println("HLOD @${address.toAddress()} <${value.truncate()}>")

        pushStack(value)
    }

    override fun halod() {
        val address = getHeapAddress()
        val size = memory[address].toInt()

        val elements = MutableList(size + 1) { memory[address + it] }

        println("HALOD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }
    }

    override fun sto() {
        val address = getAddress()
        val value = popStack()

        println("STO @${address.toAddress()} <${value.truncate()}>")

        memory[address] = value
    }

    override fun asto() {
        val address = getAddress()
        val size = popStack()

        val elements = MutableList(size.toInt()) {
            popStack()
        }

        elements.add(0, size)

        println("ASTO @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (offset in elements.indices) {
            memory[address + offset] = elements[offset]
        }
    }

    override fun hsto() {
        val address = getHeapAddress()
        val value = popStack()

        println("HSTO @${address.toAddress()} <${value.truncate()}>")

        memory[address] = value
    }

    override fun hasto() {
        val address = getHeapAddress()
        val size = popStack()

        val elements = MutableList(size.toInt()) {
            popStack()
        }

        elements.add(0, size)

        println("HASTO @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (offset in elements.indices) {
            memory[address + offset] = elements[offset]
        }
    }

    override fun alloc() {
        val id = fetchInt()

        val size = peekStackInt()

        val address = allocateMemory(id, size)

        println("ALLOC #$id <$size, @${address.toAddress()}>")
    }

    override fun realloc() {
        val id = fetchInt()

        val size = peekStackInt()

        val address = reallocateMemory(id, size)

        println("ALLOC #$id <$size, @${address.toAddress()}>")
    }

    override fun free() {
        val id = fetchInt()

        println("FREE #$id")

        freeMemory(id)
    }

    override fun size() {
        val address = getAddress()

        val totalSize = memory[address].toInt()

        println("SIZE @${address.toAddress()} <$totalSize>")

        pushStack(totalSize.toDouble())
    }

    override fun asize() {
        val address = getAddress()

        val totalSize = memory[address].toInt() / (memory[address + 1] + 1).toInt()

        println("ASIZE @${address.toAddress()} <$totalSize>")

        pushStack(totalSize.toDouble())
    }

    override fun hsize() {
        val address = getHeapAddress()

        val totalSize = memory[address].toInt()

        println("HSIZE @${address.toAddress()} <$totalSize>")

        pushStack(totalSize.toDouble())
    }

    override fun hasize() {
        val address = getHeapAddress()

        val totalSize = memory[address].toInt() / (memory[address + 1] + 1).toInt()

        println("HASIZE @${address.toAddress()} <$totalSize>")

        pushStack(totalSize.toDouble())
    }

    override fun call() {
        val index = fetchInt()

        val address = instructionPointerOrigin + index

        val last = instructionPointer

        println("CALL @${index.toAddress()}")

        instructionPointer = address
        pushCall(last.toDouble())
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

    override fun arg() {
        val argPointer = stackPointer

        pushCall(argPointer.toDouble())

        println("ARG <@${argPointer.toAddress()}>")
    }

    override fun sys() {
        val id = fetchInt()

        val function = Linker[id]

        val arguments = mutableListOf<Double>()

        val argPointer = popCall()

        while (stackPointer > argPointer) {
            arguments.add(popStack())
        }

        println("SYS #$id <${arguments.joinToString()}>")

        val result = function(this, arguments)

        for (value in result.reversed()) {
            pushStack(value)
        }
    }
}