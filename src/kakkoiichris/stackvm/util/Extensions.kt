package kakkoiichris.stackvm.util

import kotlin.math.absoluteValue

fun Double.truncate() =
    if (this - toInt() == 0.0)
        toInt().toString()
    else
        toString()

val Double.bool get() = this != 0.0

val Boolean.float get() = if (this) 1.0 else 0.0

fun Int.toAddress() =
    if (this < 0)
        "-${absoluteValue.toString().uppercase()}"
    else
        toString().uppercase()