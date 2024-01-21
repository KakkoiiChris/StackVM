package kakkoiichris.stackvm.util

import kotlin.math.abs
import kotlin.math.log10

fun Float.truncate(): String {
    if (this - toInt() == 0F) {
        return toInt().toString()
    }

    return toString()
}

fun Int.length() = when {
    this == 0 -> 1
    this < 0  -> log10(abs(toFloat())).toInt() + 2
    else      -> log10(abs(toFloat())).toInt() + 1
}

fun Float.toBool() = this != 0F

fun Boolean.toFloat() = if (this) 1F else 0F