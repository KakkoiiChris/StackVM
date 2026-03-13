package kakkoiichris.svml

import kakkoiichris.svml.lang.Directory
import kakkoiichris.svml.util.SVMLError
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

fun main(args: Array<String>) {
    val (srcFile, dstFile) = args

    try {
        compile(srcFile, dstFile)
    }
    catch (e: SVMLError) {
        System.err.println(e.message)

        e.printStackTrace()
    }
}

private fun compile(srcName: String, dstName: String) {
    val srcFile = File(srcName)

    Directory.root = srcFile.parentFile

    if (!srcFile.exists()) {
        error("Cannot load source file!")
    }

    val values = convert(srcFile)

    val dstFile = File(dstName)

    if (!dstFile.exists() && !dstFile.createNewFile()) {
        error("Cannot create destination file!")
    }

    val out = DataOutputStream(BufferedOutputStream(FileOutputStream(dstFile, false)))

    out.writeInt(values.size)

    for (value in values) {
        out.writeDouble(value)
    }

    out.close()
}