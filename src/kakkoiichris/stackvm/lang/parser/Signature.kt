package kakkoiichris.stackvm.lang.parser

data class Signature(val name: Node.Name, val params: List<DataType>) {
    val arity = params.size

    override fun toString() = params.joinToString(
        prefix = "${name.name.value}(",
        separator = ",",
        postfix = ")"
    )
}