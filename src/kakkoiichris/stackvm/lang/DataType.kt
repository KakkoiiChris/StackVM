package kakkoiichris.stackvm.lang

sealed interface DataType {
    data object Inferred : DataType

    enum class Primitive : DataType {
        VOID,
        BOOL,
        INT,
        FLOAT,
        CHAR
    }
}
