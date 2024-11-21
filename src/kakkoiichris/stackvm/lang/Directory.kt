package kakkoiichris.stackvm.lang

import kakkoiichris.stackvm.FileSource
import java.io.File

object Directory : FileSource {
    lateinit var root: File

    override fun getFile(name: String) =
        File(root, "$name.svml")
}