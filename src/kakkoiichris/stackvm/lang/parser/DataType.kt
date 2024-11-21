package kakkoiichris.stackvm.lang.parser

import kakkoiichris.stackvm.lang.lexer.Location
import kakkoiichris.stackvm.lang.lexer.TokenType

sealed interface DataType {
    val offset get() = 1

    val isHeapAllocated get() = false

    enum class Primitive : DataType {
        VOID,
        BOOL,
        INT,
        FLOAT,
        CHAR;

        override fun toString() = name.lowercase()
    }

    data class Alias(val name: Node.Name) : DataType {
        override val offset get() = getAlias(name).offset

        override val isHeapAllocated get() = getAlias(name).isHeapAllocated

        override fun toString() = getAlias(name).toString()

        companion object {
            fun of(name: String): Alias {
                val nameToken = TokenType.Name(name)

                val nameNode = Node.Name(Location.none(), nameToken)

                return Alias(nameNode)
            }
        }
    }

    data class User(val name: Node.Name) : DataType

    data class Array(val subType: DataType, val size: Int) : DataType {
        override val offset get() = (size * subType.offset) + 1

        override val isHeapAllocated get() = size == -1 || subType.isHeapAllocated

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
            "$subType[${if (size >= 1) size else ""}]"
    }

    companion object {
        private val aliases = mutableMapOf<String, DataType>()

        val string = Array(Primitive.CHAR, -1)

        fun hasAlias(name: Node.Name) =
            name.name.value in aliases

        fun getAlias(name: Node.Name) =
            aliases[name.name.value] ?: error("No type alias called '${name.name.value}' @ ${name.location}!")

        fun addAlias(name: Node.Name, type: Type) {
            if (name.name.value in aliases) {
                error("Redefined type alias '${name.name.value}' @ ${name.location}!")
            }

            aliases[name.name.value] = type.type.value
        }

        fun isEquivalent(a: DataType?, b: DataType?): Boolean = when (a) {
            null         -> false

            is Primitive -> when (b) {
                is Primitive -> a == b

                is Alias     -> isEquivalent(a, getAlias(b.name))

                else         -> false
            }

            is Alias     -> when (b) {
                is Alias -> a.name.name.value == b.name.name.value

                else     -> isEquivalent(getAlias(a.name), b)
            }

            is User      -> when (b) {
                is User -> a.name.name.value == b.name.name.value

                else    -> isEquivalent(getAlias(a.name), b)
            }

            is Array     -> when (b) {
                is Array -> (a.size == b.size || a.isHeapAllocated || b.isHeapAllocated)
                    && isEquivalent(a.subType, b.subType)

                is Alias -> isEquivalent(a, getAlias(b.name))

                else     -> false
            }
        }

        fun isArray(t: DataType?): Boolean {
            t ?: return false

            if (t is Alias) return getAlias(t.name) is Array

            return t is Array
        }
    }
}

data class Type(val location: Location, val type: TokenType.Type)