package kakkoiichris.stackvm.lang.lexer

data class Location(val file: String, val row: Int, val col: Int) {
    override fun toString() =
        "$file (Row $row, Column $col)"

    companion object {
        fun none(file: String = "") =
            Location(file, -1, -1)
    }
}