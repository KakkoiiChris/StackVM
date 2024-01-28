package kakkoiichris.stackvm.lang

sealed interface DataType {
    enum class Primitive : DataType {
        VOID,
        BOOL,
        INT,
        FLOAT,
        CHAR
    }

    data class Array(val subType: DataType, val size: Int) : DataType {
        override fun toString() =
            "$subType[$size]"
    }
}
