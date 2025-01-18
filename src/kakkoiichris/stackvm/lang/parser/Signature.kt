package kakkoiichris.stackvm.lang.parser

data class Signature(val name: Node.Name, val params: List<DataType>) {
    override fun toString() = params.joinToString(
        prefix = "${name.name.value}(",
        separator = ",",
        postfix = ")"
    ) { it.getString(name.context.source) }
}