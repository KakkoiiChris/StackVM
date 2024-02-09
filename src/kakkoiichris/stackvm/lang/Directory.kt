package kakkoiichris.stackvm.lang

import java.io.File

object Directory {
    lateinit var root: File

    fun getFile(name: String) =
        File(root, "$name.svml")
}