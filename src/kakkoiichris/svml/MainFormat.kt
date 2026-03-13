package kakkoiichris.svml

import kakkoiichris.svml.lang.Directory
import kakkoiichris.svml.lang.compiler.BytecodeFormatter
import kakkoiichris.svml.util.SVMLError
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

fun main(args: Array<String>) {
    val (srcFile, dstFile) = args

    try {
        format(srcFile, dstFile)
    }
    catch (e: SVMLError) {
        System.err.println(e.message)

        e.printStackTrace()
    }
}

private fun format(srcName: String, dstName: String) {
    val srcFile = File(srcName)

    Directory.root = srcFile.parentFile

    if (!srcFile.exists()) error("Cannot load source file!")

    val formatter = BytecodeFormatter(srcFile)

    val format = formatter.format()

    val dstFile = File(dstName)

    if (!dstFile.exists() && !dstFile.createNewFile()) {
        error("Cannot create destination file!")
    }

    val out = BufferedWriter(FileWriter(dstFile, false))

    out.write(format)

    out.close()
}