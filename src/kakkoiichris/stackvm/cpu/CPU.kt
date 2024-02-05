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

    protected var running by Register.Bool(RUN_ADR)

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

    data class Config(
        val memorySize: Int = 0x10000,
        val maxCalls: Int = 10_000,
        val maxStack: Int = 10_000
    )
}
