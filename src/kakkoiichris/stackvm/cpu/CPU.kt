package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.asm.ASMToken
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

    fun initialize(tokenizer: Iterator<ASMToken>) {
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

        when (ASMToken.Instruction.entries[index]) {
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

            ASMToken.Instruction.GLOBAL  -> global()

            ASMToken.Instruction.LOAD    -> load()

            ASMToken.Instruction.ALOAD   -> aload()

            ASMToken.Instruction.ILOAD   -> iload()

            ASMToken.Instruction.IALOAD  -> iaload()

            ASMToken.Instruction.STORE   -> store()

            ASMToken.Instruction.ASTORE  -> astore()

            ASMToken.Instruction.ISTORE  -> istore()

            ASMToken.Instruction.IASTORE -> iastore()

            ASMToken.Instruction.CALL    -> call()

            ASMToken.Instruction.RET     -> ret()

            ASMToken.Instruction.FRAME   -> frame()

            ASMToken.Instruction.SYS     -> sys()
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
