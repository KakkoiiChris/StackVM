package kakkoiichris.stackvm

import java.io.File

interface FileSource {
    fun getFile(name: String): File
}