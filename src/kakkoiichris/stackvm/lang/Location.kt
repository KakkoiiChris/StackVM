package kakkoiichris.stackvm.lang

data class Location(val row: Int, val col: Int) {
    override fun toString() =
        "Row $row, Column $col"

    companion object {
        val none get() = Location(-1, -1)
    }
}