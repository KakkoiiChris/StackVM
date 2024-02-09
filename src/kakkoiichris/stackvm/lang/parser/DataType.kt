package kakkoiichris.stackvm.lang.parser

sealed interface DataType {
    val offset get() = 1

    enum class Primitive : DataType {
        VOID,
        BOOL,
        INT,
        FLOAT,
        CHAR
    }

    data class Array(val subType: DataType, val size: Int) : DataType {
        override val offset get() = (size * subType.offset) + 1

        val dimension: Int
            get() {
                if (subType is Array) {
                    return 1 + subType.dimension
                }

                return 1
            }

        override fun toString() =
            "$subType[$size]"
    }
}
