package kakkoiichris.stackvm.lang

sealed interface DataType {
    enum class Primitive : DataType {
        VOID,
        BOOL,
        INT,
        FLOAT,
        CHAR
    }
}
