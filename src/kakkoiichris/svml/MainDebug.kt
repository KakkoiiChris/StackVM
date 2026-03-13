package kakkoiichris.svml

import kakkoiichris.svml.cpu.DebugCPU
import kakkoiichris.svml.cpu.ReleaseCPU
import kakkoiichris.svml.linker.Linker
import kakkoiichris.svml.util.SVMLError
import kakkoiichris.svml.util.truncate
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.time.measureTimedValue

fun main(args: Array<String>) {
    val srcName = args.first()

    try {
        run(srcName)
    }
    catch (e: SVMLError) {
        System.err.println(e.message)

        e.printStackTrace()
    }
}

private fun run(srcName: String) {
    val srcFile = File(srcName)

    val `in` = DataInputStream(BufferedInputStream(FileInputStream(srcFile)))

    val length = `in`.readInt()

    val values = DoubleArray(length)

    for (i in 0 until length) {
        values[i] = `in`.readDouble()
    }

    DebugCPU.initialize(values)

    Linker.link()

    val (result, runTime) = measureTimedValue { DebugCPU.run() }

    println("\n< ${result.truncate()} (${runTime.inWholeNanoseconds / 1E9}s)\n")
}