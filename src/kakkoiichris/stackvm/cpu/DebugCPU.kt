package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.asm.ASMToken
import kakkoiichris.stackvm.util.toAddress
import kakkoiichris.stackvm.util.toBool
import kakkoiichris.stackvm.util.toFloat
import kakkoiichris.stackvm.util.truncate

object DebugCPU : CPU() {
    override fun initialize(instructions: FloatArray) {
        memory = FloatArray(config.memorySize)

        running = true
        result = Float.NaN

        instructionPointer = 10
        instructionPointerOrigin = instructionPointer

        var i = instructionPointer

        for (value in instructions) {
            memory[i++] = value
        }

        callPointer = i
        callPointerOrigin = callPointer

        i += config.maxCalls

        stackPointer = i
        stackPointerOrigin = stackPointer

        i += config.maxStack

        framePointer = i
        framePointerOrigin = framePointer
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

    private fun popStackInt() = popStack().toInt()

    private fun popStackBool() = popStack().toBool()


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
        while (running) {
            Debug {
                for (i in framePointerOrigin until framePointerOrigin + 20) {
                    print("${memory[i].truncate()} ")
                }

                println()
            }

            when (ASMToken.Instruction.entries[fetchInt()]) {
                ASMToken.Instruction.HALT    -> halt()

                ASMToken.Instruction.PUSH    -> push()

                ASMToken.Instruction.POP     -> pop()

                ASMToken.Instruction.DUP     -> dup()

                ASMToken.Instruction.ADD     -> add()

                ASMToken.Instruction.SUB     -> sub()

                ASMToken.Instruction.MUL     -> mul()

                ASMToken.Instruction.DIV     -> div()

                ASMToken.Instruction.IDIV    -> idiv()

                ASMToken.Instruction.MOD     -> mod()

                ASMToken.Instruction.IMOD    -> imod()

                ASMToken.Instruction.NEG     -> neg()

                ASMToken.Instruction.AND     -> and()

                ASMToken.Instruction.OR      -> or()

                ASMToken.Instruction.NOT     -> not()

                ASMToken.Instruction.EQU     -> equ()

                ASMToken.Instruction.GRT     -> grt()

                ASMToken.Instruction.GEQ     -> geq()

                ASMToken.Instruction.JMP     -> jmp()

                ASMToken.Instruction.JIF     -> jif()

                ASMToken.Instruction.LOAD    -> load()

                ASMToken.Instruction.ALOAD   -> aload()

                ASMToken.Instruction.ILOAD   -> iload()

                ASMToken.Instruction.IALOAD  -> iaload()

                ASMToken.Instruction.LOADG   -> loadg()

                ASMToken.Instruction.ALOADG  -> aloadg()

                ASMToken.Instruction.ILOADG  -> iloadg()

                ASMToken.Instruction.IALOADG -> ialoadg()

                ASMToken.Instruction.STORE   -> store()

                ASMToken.Instruction.ASTORE  -> astore()

                ASMToken.Instruction.ISTORE  -> istore()

                ASMToken.Instruction.IASTORE -> iastore()

                ASMToken.Instruction.CALL    -> call()

                ASMToken.Instruction.RET     -> ret()

                ASMToken.Instruction.FRAME   -> frame()

                ASMToken.Instruction.SYS     -> sys()
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

    private fun halt() {
        result = popStack()

        Debug.println("HALT #${result.truncate()}")

        running = false
    }

    private fun push() {
        val value = fetch()

        Debug.println("PUSH #${value.truncate()}")

        pushStack(value)
    }

    private fun pop() {
        val value = popStack()

        Debug.println("POP <${value.truncate()}>")
    }

    private fun dup() {
        val value = peekStack()

        Debug.println("DUP <${value.truncate()}>")

        pushStack(value)
    }

    private fun add() {
        val b = popStack()
        val a = popStack()

        val value = a + b

        Debug.println("ADD #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    private fun sub() {
        val b = popStack()
        val a = popStack()

        val value = a - b

        Debug.println("SUB #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    private fun mul() {
        val b = popStack()
        val a = popStack()

        val value = a * b

        Debug.println("MUL #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    private fun div() {
        val b = popStack()
        val a = popStack()

        val value = a / b

        Debug.println("DIV #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    private fun idiv() {
        val b = popStack()
        val a = popStack()

        val value = (a.toInt() / b.toInt()).toFloat()

        Debug.println("IDIV #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    private fun mod() {
        val b = popStack()
        val a = popStack()

        val value = a % b

        Debug.println("MOD #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    private fun imod() {
        val b = popStack()
        val a = popStack()

        val value = (a.toInt() % b.toInt()).toFloat()

        Debug.println("IMOD #${a.truncate()} #${b.truncate()} <$value>")

        pushStack(value)
    }

    private fun neg() {
        val a = popStack()

        val value = -a

        Debug.println("NEG #${value.truncate()}")

        pushStack(value)
    }

    private fun and() {
        val b = popStack()
        val a = popStack()

        Debug.println("AND #${a.truncate()} #${b.truncate()}")

        pushStack((a.toBool() && b.toBool()).toFloat())
    }

    private fun or() {
        val b = popStack()
        val a = popStack()

        Debug.println("OR #${a.truncate()} #${b.truncate()}")

        pushStack((a.toBool() || b.toBool()).toFloat())
    }

    private fun not() {
        val value = popStack()

        Debug.println("NOT #${value.truncate()}")

        pushStack((!value.toBool()).toFloat())
    }

    private fun equ() {
        val b = popStack()
        val a = popStack()

        Debug.println("EQU #${a.truncate()} #${b.truncate()}")

        pushStack((a == b).toFloat())
    }

    private fun grt() {
        val b = popStack()
        val a = popStack()

        Debug.println("GRT #${a.truncate()} #${b.truncate()}")

        pushStack((a > b).toFloat())
    }

    private fun geq() {
        val b = popStack()
        val a = popStack()

        Debug.println("GEQ #${a.truncate()} #${b.truncate()}")

        pushStack((a >= b).toFloat())
    }

    private fun jmp() {
        val address = instructionPointerOrigin + fetchInt()

        Debug.println("JMP @${address.toAddress()}")

        instructionPointer = address
    }

    private fun jif() {
        val address = instructionPointerOrigin + fetchInt()

        Debug.println("JIF @${address.toAddress()}")

        if (popStackBool()) {
            instructionPointer = address
        }
    }

    private fun load() {
        val address = fetchInt() + framePointer
        val value = memory[address]

        Debug.println("LOAD @${address.toAddress()} <${value.truncate()}>")

        pushStack(value)
    }

    private fun aload() {
        val address = fetchInt() + framePointer
        val size = memory[address]

        val elements = FloatArray(size.toInt()) { memory[address + 1 + it] }

        Debug.println("ALOAD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }

        pushStack(size)
    }

    private fun iload() {
        var address = fetchInt() + framePointer
        val indexCount = fetchInt()

        var indexOffset = 0

        for (i in 0..<indexCount) {
            val subSize = memory[address + indexOffset + 1].toInt()

            val index = popStackInt()

            indexOffset = (indexOffset + 1) + (index * subSize)
        }

        address += indexOffset

        val value = memory[address]

        Debug.println("ILOAD @${address.toAddress()} #$indexCount <${value.truncate()}>")

        pushStack(value)
    }

    private fun iaload() {
        var address = fetchInt() + framePointer
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

        Debug.println("IALOAD @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }

        pushStack(size)
    }

    private fun loadg() {
        val address = fetchInt() + framePointerOrigin
        val value = memory[address]

        Debug.println("LOADG @${address.toAddress()} <${value.truncate()}>")

        pushStack(value)
    }

    private fun aloadg() {
        val address = fetchInt() + framePointerOrigin
        val size = memory[address]

        val elements = FloatArray(size.toInt()) { memory[address + 1 + it] }

        Debug.println("ALOADG @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }

        pushStack(size)
    }

    private fun iloadg() {
        var address = fetchInt() + framePointerOrigin
        val indexCount = fetchInt()

        var indexOffset = 0

        for (i in 0..<indexCount - 1) {
            val subSize = memory[address + indexOffset + 1].toInt()

            val index = popStackInt()

            indexOffset = (indexOffset + 1) + (index * (subSize + 1))
        }

        val index = popStackInt()

        indexOffset = (indexOffset + 1) + index

        address += indexOffset

        val value = memory[address]

        Debug.println("ILOADG @${address.toAddress()} #$indexCount <${value.truncate()}>")

        pushStack(value)
    }

    private fun ialoadg() {
        var address = fetchInt() + framePointerOrigin
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

        Debug.println("IALOADG @${address.toAddress()} [${elements.joinToString(separator = ",") { it.truncate() }}]")

        for (element in elements.reversed()) {
            pushStack(element)
        }

        pushStack(size)
    }

    private fun store() {
        val address = fetchInt() + framePointer
        val value = popStack()

        Debug.println("STORE @${address.toAddress()} <${value.truncate()}>")

        memory[address] = value
    }

    private fun astore() {
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

    private fun istore() {
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

        Debug.println("ISTORE @${address.toAddress()} #$indexCount <${value.truncate()}>")

        memory[address] = value
    }

    private fun iastore() {
        TODO("IASTORE")
    }

    private fun call() {
        val address = instructionPointer + 1
        instructionPointer = instructionPointerOrigin + fetchInt()

        Debug.println("CALL @${instructionPointer.toAddress()}")

        pushCall(address.toFloat())
    }

    private fun ret() {
        val address = popCall()

        Debug.println("RET @${address.toAddress()}")

        if (address < 0) {
            result = popStack()

            running = false

            return
        }

        instructionPointer = address

        popFrame()
    }

    private fun frame() {
        val value = fetchInt()

        Debug.println("FRAME $$value")

        pushFrame(value)
    }

    private fun sys() {
        val id = fetchInt()

        val function = SystemFunctions[id]

        val args = mutableListOf<Float>()

        repeat(function.signature.arity) {
            args.add(popStack())
        }

        Debug.println("SYS #$id <${args.joinToString()}>")

        val result = function(args)

        for (value in result.reversed()) {
            pushStack(value)
        }
    }
}