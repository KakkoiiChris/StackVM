package kakkoiichris.stackvm.util

import kotlin.math.absoluteValue

fun Float.truncate() =
    if (this - toInt() == 0F)
        toInt().toString()
    else
        toString()

val Float.bool get() = this != 0F

val Boolean.float get() = if (this) 1F else 0F

fun Int.toAddress() =
    if (this < 0)
        "-0x${absoluteValue.toString(16).uppercase()}"
    else
        "0x${toString(16).uppercase()}"