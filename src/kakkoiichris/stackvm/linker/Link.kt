package kakkoiichris.stackvm.linker

interface Link {
    val name: String

    fun open(linker: Linker)

    fun close(linker: Linker)
}