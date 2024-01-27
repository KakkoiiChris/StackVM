package kakkoiichris.stackvm.lang

data class Signature(val name: Node.Name, val params: List<DataType>) {
    val arity = params.size

    override fun equals(other: Any?): Boolean {
        if (other !is Signature) return true

        if (name != other.name) return false

        for ((a, b) in params.zip(other.params)) {
            if (a != b) return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + params.hashCode()
        return result
    }

    override fun toString() = params.joinToString(prefix = "${name.name.value}(", separator = ",", postfix = ")")
}