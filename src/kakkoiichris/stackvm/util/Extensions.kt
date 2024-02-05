package kakkoiichris.stackvm.util

import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.log10

fun Float.truncate() =
    if (this - toInt() == 0F)
        toInt().toString()
    else
        toString()

fun Int.toAddress() =
    if (this < 0)
        "-0x${absoluteValue.toString(16)}"
    else
        "0x${toString(16)}"

fun Int.length() = when {
    this == 0 -> 1
    this < 0  -> log10(abs(toFloat())).toInt() + 2
    else      -> log10(abs(toFloat())).toInt() + 1
}

fun Float.toBool() = this != 0F

fun Boolean.toFloat() = if (this) 1F else 0F