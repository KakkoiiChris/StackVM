package kakkoiichris.stackvm.cpu

abstract class CPU(protected val config: Config = Config()) {
    protected val memory = FloatArray(config.memorySize)

    abstract fun load(values: FloatArray)

    abstract fun run(): Float

    data class Config(
        val memorySize: Int = 0x10000,
        val maxCalls: Int = 10_000,
        val maxStack: Int = 10_000
    )
}