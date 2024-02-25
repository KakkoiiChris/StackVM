package kakkoiichris.stackvm.linker

interface Link {
    val name: String

    fun open(library: Linker)

    fun close(library: Linker)
}