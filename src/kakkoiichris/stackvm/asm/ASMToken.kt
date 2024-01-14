package kakkoiichris.stackvm.asm

data class ASMToken(val type: ASMTokenType) {
    fun toFloat()=
        type.value
}