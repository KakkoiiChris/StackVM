package kakkoiichris.stackvm.cpu

data class Config(
    val memorySize: Int = 0x10000,
    val maxCalls: Int = 10_000,
    val maxStack: Int = 10_000
)