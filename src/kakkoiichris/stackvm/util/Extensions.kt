package kakkoiichris.stackvm.util

fun Float.truncate() =
    if (this - toInt() == 0F)
        toInt().toString()
    else
        toString()

val Float.bool get() = this != 0F

val Boolean.float get() = if (this) 1F else 0F