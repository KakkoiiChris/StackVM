package kakkoiichris.stackvm.cpu

abstract class CPU(config: Config = Config()) {
    protected val memory = FloatArray(config.memorySize)

    abstract fun load(values: FloatArray)

    abstract fun run(): Float

    data class Config(
        val memorySize: Int = 0x10000
    )
}