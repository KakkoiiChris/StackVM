package kakkoiichris.stackvm.lang.parser

sealed interface DataType {
    fun getOffset() = 1

    enum class Primitive : DataType {
        VOID,
        BOOL,
        INT,
        FLOAT,
        CHAR;

        override fun toString() = name.lowercase()
    }

    data class Alias(val name: Node.Name) : DataType {
        override fun getOffset() = getAlias(name).getOffset()

        override fun toString() = "${name.name.value}=${getAlias(name)}"
    }

    data class User(val name: Node.Name) : DataType

    data class Array(val subType: DataType, val size: Int) : DataType {
        override fun getOffset() = (size * subType.getOffset()) + 1

        val dimension: Int
            get() {
                if (subType is Array) {
                    return 1 + subType.dimension
                }

                return 1
            }

        val sizes: IntArray
            get() {
                if (subType is Array) {
                    return intArrayOf(*subType.sizes, size)
                }

                return intArrayOf(size)
            }

        override fun toString() =
            "$subType[$size]"
    }

    companion object {
        private val aliases = mutableMapOf<String, DataType>()

        fun hasAlias(name: Node.Name) =
            name.name.value in aliases

        fun getAlias(name: Node.Name) =
            aliases[name.name.value] ?: error("No type alias called '${name.name.value}' @ ${name.location}!")

        fun addAlias(name: Node.Name, type: Node.Type) {
            if (name.name.value in aliases) {
                error("Redefined type alias '${name.name.value}' @ ${name.location}!")
            }

            aliases[name.name.value] = type.type.value
        }

        fun isEqual(a: DataType, b: DataType): Boolean = when (a) {
            is Primitive -> when (b) {
                is Primitive -> a == b

                is Alias     -> isEqual(a, getAlias(b.name))

                else         -> false
            }

            is Alias     -> when (b) {
                is Alias -> a.name.name.value == b.name.name.value

                else     -> isEqual(getAlias(a.name), b)
            }

            is User      -> when (b) {
                is User -> a.name.name.value == b.name.name.value

                else    -> isEqual(getAlias(a.name), b)
            }

            is Array     -> when (b) {
                is Array -> a.size == b.size && isEqual(a.subType, b.subType)

                is Alias -> isEqual(a, getAlias(b.name))

                else     -> false
            }
        }

        fun isArray(t: DataType): Boolean {
            if (t is Alias) return getAlias(t.name) is Array

            return t is Array
        }

        fun asArray(t: DataType): Array {
            if (t is Alias) return getAlias(t.name) as Array

            return t as Array
        }
    }
}
