/*   ______  ____   ____  ____    ____  _____
 * .' ____ \|_  _| |_  _||_   \  /   _||_   _|
 * | (___ \_| \ \   / /    |   \/   |    | |
 *  _.____`.   \ \ / /     | |\  /| |    | |   _
 * | \____) |   \ ' /     _| |_\/_| |_  _| |__/ |
 *  \______.'    \_/     |_____||_____||________|
 *
 *         Stack Virtual Machine Language
 *     Copyright (C) 2024 Christian Alexander
 */
package kakkoiichris.svml.lang.parser

import kakkoiichris.svml.lang.Source
import kakkoiichris.svml.lang.lexer.Context
import kakkoiichris.svml.util.svmlError

sealed interface DataType {
    fun getOffset(source: Source) = 1

    fun isHeapAllocated(source: Source) = false

    fun getString(source: Source) = ""

    enum class Primitive : DataType {
        VOID,
        BOOL,
        INT,
        FLOAT,
        CHAR;

        override fun getString(source: Source) = name.lowercase()
    }

    data class Alias(val name: Node.Name) : DataType {
        override fun getOffset(source: Source) = getAlias(name, source).getOffset(source)

        override fun isHeapAllocated(source: Source) = getAlias(name, source).isHeapAllocated(source)

        override fun getString(source: Source) = getAlias(name, source).getString(source)

        companion object {
            fun of(name: String): Alias {
                val nameNode = Node.Name(Context.none(), name)

                return Alias(nameNode)
            }
        }
    }

    data class User(val name: Node.Name) : DataType {
        override fun getOffset(source: Source) = 0//(size * subType.getOffset(source)) + 1

        override fun isHeapAllocated(source: Source) = false//size == -1 || subType.isHeapAllocated(source)

        override fun getString(source: Source) = name.value
    }

    data class Array(val subType: DataType, val size: Int) : DataType {
        override fun getOffset(source: Source) = (size * subType.getOffset(source)) + 1

        override fun isHeapAllocated(source: Source) = size == -1 || subType.isHeapAllocated(source)

        override fun getString(source: Source) = "${subType.getString(source)}[]"

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
            name.value in aliases

        fun getAlias(name: Node.Name, source: Source) =
            aliases[name.value] ?: svmlError("No type alias called '${name.value}'", source, name.context)

        fun addAlias(name: Node.Name, type: Type, source: Source) {
            if (name.value in aliases) {
                svmlError("Redefined type alias '${name.value}' @ ${name.context}!", source, name.context)
            }

            aliases[name.value] = type.value
        }

        fun isEquivalent(a: DataType?, b: DataType?, source: Source): Boolean = when (a) {
            null         -> false

            is Primitive -> when (b) {
                is Primitive -> a == b

                is Alias     -> isEquivalent(a, getAlias(b.name, source), source)

                else         -> false
            }

            is Alias     -> when (b) {
                is Alias -> a.name.value == b.name.value

                else     -> isEquivalent(getAlias(a.name, source), b, source)
            }

            is User      -> when (b) {
                is User -> a.name.value == b.name.value

                else    -> isEquivalent(getAlias(a.name, source), b, source)
            }

            is Array     -> when (b) {
                is Array -> (a.size == b.size || a.isHeapAllocated(source) || b.isHeapAllocated(source))
                    && isEquivalent(a.subType, b.subType, source)

                is Alias -> isEquivalent(a, getAlias(b.name, source), source)

                else     -> false
            }
        }

        fun isArray(t: DataType?, source: Source): Boolean {
            t ?: return false

            if (t is Alias) return getAlias(t.name, source) is Array

            return t is Array
        }

        fun asArray(t: DataType, source: Source): Array {
            if (t is Alias) return asArray(getAlias(t.name, source), source)

            return t as Array
        }
    }
}

data class Type(val context: Context, val value: DataType)