package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.asm.ASMToken

abstract class CPU(protected val config: Config = Config()) {
    companion object {
        private const val RUN_ADR = 0
        private const val RES_ADR = 1
        private const val IPO_ADR = 2
        private const val IPA_ADR = 3
        private const val SPO_ADR = 4
        private const val SPA_ADR = 5
        private const val FPO_ADR = 6
        private const val FPA_ADR = 7
        private const val CPO_ADR = 8
        private const val CPA_ADR = 9
    }

    internal lateinit var memory: FloatArray

    protected var running by BoolRegister(RUN_ADR)

    protected var result by FloatRegister(RES_ADR)

    protected var instructionPointerOrigin by IntRegister(IPO_ADR)
    protected var instructionPointer by IntRegister(IPA_ADR)

    protected var stackPointerOrigin by IntRegister(SPO_ADR)
    protected var stackPointer by IntRegister(SPA_ADR)

    protected var framePointerOrigin by IntRegister(FPO_ADR)
    protected var framePointer by IntRegister(FPA_ADR)

    protected var callPointerOrigin by IntRegister(CPO_ADR)
    protected var callPointer by IntRegister(CPA_ADR)

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
}