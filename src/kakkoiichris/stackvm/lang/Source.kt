package kakkoiichris.stackvm.lang

import java.io.File

data class Source(val name: String, val text: String) {
    fun getLine(row: Int) =
        text.lines()[row - 1]

    companion object {
        fun of(file: File) =
            Source(file.name, file.readText())
    }
}