package kakkoiichris.stackvm.lang

import java.io.File

data class Source(val name: String, val text: String) {
    companion object {
        fun of(file: File) =
            Source(file.name, file.readText())
    }
}