package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.lang.compiler.Bytecode
import kakkoiichris.stackvm.util.bool

abstract class CPU(protected val config: Config = Config()) {
    companion object {
        private var initAddress = 0

        private val RUN_ADR = initAddress++
        private val GLO_ADR = initAddress++
        private val RES_ADR = initAddress++
        private val IPO_ADR = initAddress++
        private val IPA_ADR = initAddress++
        private val SPO_ADR = initAddress++
        private val SPA_ADR = initAddress++
        private val FPO_ADR = initAddress++
        private val FPA_ADR = initAddress++
        private val CPO_ADR = initAddress++
        private val CPA_ADR = initAddress++
        private val TPO_ADR = initAddress++
        private val TPA_ADR = initAddress++
        private val HPO_ADR = initAddress++
        private val HPA_ADR = initAddress++
    }

    internal lateinit var memory: DoubleArray

    internal var running by Register.Bool(RUN_ADR)

    protected var global by Register.Bool(GLO_ADR)

    internal var result by Register.Float(RES_ADR)

    protected var instructionPointerOrigin by Register.Int(IPO_ADR)
    protected var instructionPointer by Register.Int(IPA_ADR)

    protected var stackPointerOrigin by Register.Int(SPO_ADR)
    protected var stackPointer by Register.Int(SPA_ADR)

    protected var framePointerOrigin by Register.Int(FPO_ADR)
    protected var framePointer by Register.Int(FPA_ADR)

    protected var callPointerOrigin by Register.Int(CPO_ADR)
    protected var callPointer by Register.Int(CPA_ADR)

    protected var tablePointerOrigin by Register.Int(TPO_ADR)
    protected var tablePointer by Register.Int(TPA_ADR)

    protected var heapPointerOrigin by Register.Int(HPO_ADR)
    protected var heapPointer by Register.Int(HPA_ADR)

    fun initialize(instructions: DoubleArray) {
        memory = DoubleArray(config.memorySize)

        running = true
        result = Double.NaN

        instructionPointer = initAddress
        instructionPointerOrigin = instructionPointer

        for (value in instructions) {
            memory[initAddress++] = value
        }

        callPointer = initAddress
        callPointerOrigin = callPointer

        initAddress += config.maxCalls

        stackPointer = initAddress
        stackPointerOrigin = stackPointer

        initAddress += config.maxStack

        framePointer = initAddress
        framePointerOrigin = framePointer

        val halfWay = ((memory.size - framePointerOrigin) / 2) + framePointerOrigin

        initAddress += halfWay

        tablePointerOrigin = initAddress
        tablePointer = tablePointerOrigin

        initAddress += config.maxAllocations

        heapPointerOrigin = initAddress
        heapPointer = heapPointerOrigin

        pushStack(0.0)
    }

    fun initialize(tokenizer: Iterator<Bytecode>) {
        val instructions = tokenizer
            .asSequence()
            .toList()
            .map { it.value }
            .toDoubleArray()

        initialize(instructions)
    }

    abstract fun run(): Double

    protected fun decode() {
        val index = fetchInt()

        if (index !in Bytecode.Instruction.entries.indices) error("Value '$index' is not an instruction @ $instructionPointer!")

        when (Bytecode.Instruction.entries[index]) {
            Bytecode.Instruction.HALT   -> halt()

            Bytecode.Instruction.PUSH   -> push()

            Bytecode.Instruction.POP    -> pop()

            Bytecode.Instruction.DUP    -> dup()

            Bytecode.Instruction.ADD    -> add()

            Bytecode.Instruction.SUB    -> sub()

            Bytecode.Instruction.MUL    -> mul()

            Bytecode.Instruction.DIV    -> div()

            Bytecode.Instruction.IDIV   -> idiv()

            Bytecode.Instruction.MOD    -> mod()

            Bytecode.Instruction.IMOD   -> imod()

            Bytecode.Instruction.NEG    -> neg()

            Bytecode.Instruction.AND    -> and()

            Bytecode.Instruction.OR     -> or()

            Bytecode.Instruction.NOT    -> not()

            Bytecode.Instruction.EQU    -> equ()

            Bytecode.Instruction.GRT    -> grt()

            Bytecode.Instruction.GEQ    -> geq()

            Bytecode.Instruction.JMP    -> jmp()

            Bytecode.Instruction.JIF    -> jif()

            Bytecode.Instruction.GLOB   -> glob()

            Bytecode.Instruction.LOD    -> lod()

            Bytecode.Instruction.ALOD   -> alod()

            Bytecode.Instruction.ILOD   -> ilod()

            Bytecode.Instruction.IALOD  -> ialod()

            Bytecode.Instruction.STO    -> sto()

            Bytecode.Instruction.ASTO   -> asto()

            Bytecode.Instruction.ISTO   -> isto()

            Bytecode.Instruction.IASTO  -> iasto()

            Bytecode.Instruction.ALLOC  -> alloc()

            Bytecode.Instruction.FREE   -> free()

            Bytecode.Instruction.HALOD  -> halod()

            Bytecode.Instruction.HILOD  -> hilod()

            Bytecode.Instruction.HIALOD -> hialod()

            Bytecode.Instruction.HASTO  -> hasto()

            Bytecode.Instruction.HISTO  -> histo()

            Bytecode.Instruction.HIASTO -> hiasto()

            Bytecode.Instruction.SIZE   -> size()

            Bytecode.Instruction.HSIZE  -> hsize()

            Bytecode.Instruction.CALL   -> call()

            Bytecode.Instruction.RET    -> ret()

            Bytecode.Instruction.FRAME  -> frame()

            Bytecode.Instruction.SYS    -> sys()
        }
    }

    protected fun fetch() = memory[instructionPointer++]

    protected fun fetchInt() = fetch().toInt()

    protected fun pushStack(value: Double) {
        memory[stackPointer++] = value
    }

    protected fun popStack(): Double {
        val address = --stackPointer

        if (address < stackPointerOrigin) error("Stack underflow!")

        return memory[address]
    }

    protected fun popStackInt() = popStack().toInt()

    protected fun popStackBool() = popStack().bool


    protected fun peekStack() = memory[stackPointer - 1]

    protected fun peekStackInt() = peekStack().toInt()

    protected fun pushFrame(offset: Int) {
        framePointer += offset

        memory[framePointer++] = offset.toDouble()
    }

    protected fun popFrame() {
        val offset = memory[--framePointer]

        framePointer -= offset.toInt()

        if (framePointer < framePointerOrigin) error("Frame stack underflow!")
    }

    protected fun pushCall(value: Double) {
        memory[++callPointer] = value
    }

    protected fun popCall(): Int {
        if (callPointer > callPointerOrigin) {
            return memory[callPointer--].toInt()
        }

        return -1
    }

    protected fun getLoadOffset(): Int {
        if (global) {
            global = false

            return framePointerOrigin
        }

        return framePointer
    }

    protected fun allocateMemory(id: Int, size: Int): Int {
        var address = heapPointerOrigin

        var scanning = true

        while (scanning) {
            while (memory[address] > 0) {
                address += 1 + memory[address].toInt()
            }

            for (i in 0..size) {
                if (memory[address + i] != 0.0) {
                    continue
                }
            }

            scanning = false
        }

        val tableAddress = tablePointerOrigin + id

        memory[tableAddress] = address.toDouble()

        return address
    }

    protected fun freeMemory(id: Int) {
        val tableAddress = tablePointerOrigin + id

        val heapAddress = memory[tableAddress].toInt()

        val size = memory[heapAddress].toInt()

        repeat(size + 1) { i ->
            memory[heapAddress + i] = 0.0
        }
    }

    abstract fun halt()

    abstract fun push()

    abstract fun pop()

    abstract fun dup()

    abstract fun add()

    abstract fun sub()

    abstract fun mul()

    abstract fun div()

    abstract fun idiv()

    abstract fun mod()

    abstract fun imod()

    abstract fun neg()

    abstract fun and()

    abstract fun or()

    abstract fun not()

    abstract fun equ()

    abstract fun grt()

    abstract fun geq()

    abstract fun jmp()

    abstract fun jif()

    abstract fun glob()

    abstract fun lod()

    abstract fun alod()

    abstract fun ilod()

    abstract fun ialod()

    abstract fun sto()

    abstract fun asto()

    abstract fun isto()

    abstract fun iasto()

    abstract fun alloc()

    abstract fun free()

    abstract fun halod()

    abstract fun hilod()

    abstract fun hialod()

    abstract fun hasto()

    abstract fun histo()

    abstract fun hiasto()

    abstract fun size()

    abstract fun hsize()

    abstract fun call()

    abstract fun ret()

    abstract fun frame()

    abstract fun sys()

    data class Config(
        val memorySize: Int = 100_000_000,
        val maxCalls: Int = 10_000,
        val maxStack: Int = 1_000_000,
        val maxAllocations: Int = 10_000
    )
}
