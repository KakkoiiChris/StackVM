package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.lang.compiler.Bytecode
import kakkoiichris.stackvm.util.bool

abstract class CPU(protected val config: Config = Config()) {
    companion object {
        private const val RUN_ADR = 0
        private const val GLO_ADR = 1
        private const val RES_ADR = 2
        private const val IPO_ADR = 3
        private const val IPA_ADR = 4
        private const val SPO_ADR = 5
        private const val SPA_ADR = 6
        private const val FPO_ADR = 7
        private const val FPA_ADR = 8
        private const val CPO_ADR = 9
        private const val CPA_ADR = 10
    }

    internal lateinit var memory: FloatArray

    protected var running by Register.Bool(RUN_ADR)

    protected var global by Register.Bool(GLO_ADR)

    protected var result by Register.Float(RES_ADR)

    protected var instructionPointerOrigin by Register.Int(IPO_ADR)
    protected var instructionPointer by Register.Int(IPA_ADR)

    protected var stackPointerOrigin by Register.Int(SPO_ADR)
    protected var stackPointer by Register.Int(SPA_ADR)

    protected var framePointerOrigin by Register.Int(FPO_ADR)
    protected var framePointer by Register.Int(FPA_ADR)

    protected var callPointerOrigin by Register.Int(CPO_ADR)
    protected var callPointer by Register.Int(CPA_ADR)

    abstract fun initialize(instructions: FloatArray)

    fun initialize(tokenizer: Iterator<Bytecode>) {
        val instructions = tokenizer
            .asSequence()
            .toList()
            .map { it.value }
            .toFloatArray()

        initialize(instructions)
    }

    abstract fun run(): Float

    protected fun decode() {
        val index = fetchInt()

        when (Bytecode.Instruction.entries[index]) {
            Bytecode.Instruction.HALT    -> halt()

            Bytecode.Instruction.PUSH    -> push()

            Bytecode.Instruction.POP     -> pop()

            Bytecode.Instruction.DUP     -> dup()

            Bytecode.Instruction.ADD     -> add()

            Bytecode.Instruction.SUB     -> sub()

            Bytecode.Instruction.MUL     -> mul()

            Bytecode.Instruction.DIV     -> div()

            Bytecode.Instruction.IDIV    -> idiv()

            Bytecode.Instruction.MOD     -> mod()

            Bytecode.Instruction.IMOD    -> imod()

            Bytecode.Instruction.NEG     -> neg()

            Bytecode.Instruction.AND     -> and()

            Bytecode.Instruction.OR      -> or()

            Bytecode.Instruction.NOT     -> not()

            Bytecode.Instruction.EQU     -> equ()

            Bytecode.Instruction.GRT     -> grt()

            Bytecode.Instruction.GEQ     -> geq()

            Bytecode.Instruction.JMP     -> jmp()

            Bytecode.Instruction.JIF     -> jif()

            Bytecode.Instruction.GLOBAL  -> global()

            Bytecode.Instruction.LOAD    -> load()

            Bytecode.Instruction.ALOAD   -> aload()

            Bytecode.Instruction.ILOAD   -> iload()

            Bytecode.Instruction.IALOAD  -> iaload()

            Bytecode.Instruction.STORE   -> store()

            Bytecode.Instruction.ASTORE  -> astore()

            Bytecode.Instruction.ISTORE  -> istore()

            Bytecode.Instruction.IASTORE -> iastore()

            Bytecode.Instruction.SIZE    -> size()

            Bytecode.Instruction.CALL    -> call()

            Bytecode.Instruction.RET     -> ret()

            Bytecode.Instruction.FRAME   -> frame()

            Bytecode.Instruction.SYS     -> sys()
        }
    }

    protected fun fetch() = memory[instructionPointer++]

    protected fun fetchInt() = fetch().toInt()

    protected fun pushStack(value: Float) {
        memory[stackPointer++] = value
    }

    protected fun popStack(): Float {
        val address = --stackPointer

        if (address < stackPointerOrigin) error("Stack underflow!")

        return memory[address]
    }

    protected fun popStackInt() = popStack().toInt()

    protected fun popStackBool() = popStack().bool


    protected fun peekStack() = memory[stackPointer - 1]

    protected fun pushFrame(offset: Int) {
        framePointer += offset

        memory[framePointer++] = offset.toFloat()
    }

    protected fun popFrame() {
        val offset = memory[--framePointer]

        framePointer -= offset.toInt()

        if (framePointer < framePointerOrigin) error("Frame stack underflow!")
    }

    protected fun pushCall(value: Float) {
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

    abstract fun global()

    abstract fun load()

    abstract fun aload()

    abstract fun iload()

    abstract fun iaload()

    abstract fun store()

    abstract fun astore()

    abstract fun istore()

    abstract fun iastore()

    abstract fun size()

    abstract fun call()

    abstract fun ret()

    abstract fun frame()

    abstract fun sys()

    data class Config(
        val memorySize: Int = 0x10000,
        val maxCalls: Int = 10_000,
        val maxStack: Int = 10_000
    )
}
