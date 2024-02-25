package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.linker.Linker
import kakkoiichris.stackvm.util.bool
import kakkoiichris.stackvm.util.float
import kakkoiichris.stackvm.util.toAddress
import kakkoiichris.stackvm.util.truncate

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
        val address = fetchInt() + getLoadOffset()
        val value = memory[address]

        println("LOD @${address.toAddress()} <${value.truncate()}>")

        pushStack(value)
    }

    override fun alod() {
        val address = fetchInt() + getLoadOffset()
        val size = memory[address]

        val elements = MutableList(size.toInt()) { memory[address + 1 + it] }

        elements.add(0, size)

        println("ALOD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }
    }

    override fun ilod() {
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

        println("ILOD @${address.toAddress()} #$indexCount <[${indices.joinToString(separator = "][")}], ${value.truncate()}>")

        pushStack(value)
    }

    override fun ialod() {
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

        val elements = DoubleArray(size.toInt() + 1) { memory[address + it] }

        println("IALOD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }
    }

    override fun sto() {
        val address = fetchInt() + framePointer
        val value = popStack()

        println("STO @${address.toAddress()} <${value.truncate()}>")

        memory[address] = value
    }

    override fun asto() {
        val address = fetchInt() + framePointer
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

    override fun isto() {
        var address = fetchInt() + framePointer
        val indexCount = fetchInt()

        for (i in 0 until indexCount - 1) {
            address++

            val subSize = memory[address].toInt()

            address += popStackInt() * (subSize + 1)
        }

        address += popStackInt() + 1

        val value = popStack()

        println("ISTO @${address.toAddress()} #$indexCount <${value.truncate()}>")

        memory[address] = value
    }

    override fun iasto() {
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

        println("IASTO @${address.toAddress()} #$indexCount [${elements.joinToString(separator = ",") { it.truncate() }}]")

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

    override fun halod() {
        val id = fetchInt()
        val address = memory[tablePointerOrigin + id].toInt()
        val size = memory[address]

        val elements = MutableList(size.toInt()) { memory[address + 1 + it] }

        elements.add(0, size)

        println("HALOD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }
    }

    override fun hilod() {
        val id = fetchInt()
        var address = memory[tablePointerOrigin + id].toInt()
        val indexCount = fetchInt()

        val indices = List(indexCount) { popStackInt() }

        for (index in indices.dropLast(1)) {
            address++

            val subSize = memory[address].toInt()

            address += index * (subSize + 1)
        }

        address += indices.last() + 1

        val value = memory[address]

        println("HILOD @${address.toAddress()} #$indexCount <[${indices.joinToString(separator = "][")}], ${value.truncate()}>")

        pushStack(value)
    }

    override fun hialod() {
        val id = fetchInt()
        var address = memory[tablePointerOrigin + id].toInt()
        val indexCount = fetchInt()

        val indices = List(indexCount) { popStackInt() }

        for (index in indices.dropLast(1)) {
            address++

            val subSize = memory[address].toInt()

            address += index * (subSize + 1)
        }

        address += indices.last()

        val size = memory[address]

        val elements = DoubleArray(size.toInt() + 1) { memory[address + it] }

        println("HIALOD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }
    }

    override fun hasto() {
        val id = fetchInt()
        val address = memory[tablePointerOrigin + id].toInt()
        val size = popStack()

        val elements = MutableList(size.toInt()) {
            popStack()
        }

        elements.add(0, size)

        println("HASTO #$id <@${address.toAddress()}> [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (offset in elements.indices) {
            memory[address + offset] = elements[offset]
        }
    }

    override fun histo() {
        val id = fetchInt()
        var address = memory[tablePointerOrigin + id].toInt()
        val indexCount = fetchInt()

        for (i in 0 until indexCount - 1) {
            address++

            val subSize = memory[address].toInt()

            address += popStackInt() * (subSize + 1)
        }

        address += popStackInt() + 1

        val value = popStack()

        println("HISTO @${address.toAddress()} #$indexCount <${value.truncate()}>")

        memory[address] = value
    }

    override fun hiasto() {
        val id = fetchInt()
        var address = memory[tablePointerOrigin + id].toInt()
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

        println("HIASTO @${address.toAddress()} #$indexCount [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (offset in elements.indices) {
            memory[address + offset] = elements[offset]
        }
    }

    override fun size() {
        val address = fetchInt() + getLoadOffset()

        val totalSize = memory[address].toInt()

        println("SIZE @${address.toAddress()} <$totalSize>")

        pushStack(totalSize.toDouble())
    }

    override fun hsize() {
        val id = fetchInt()
        val address = memory[tablePointerOrigin + id].toInt()

        val totalSize = memory[address].toInt()

        println("HSIZE @${address.toAddress()} <$totalSize>")

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

    override fun sys() {
        val id = fetchInt()

        val function = Linker[id]

        val args = mutableListOf<Double>()

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