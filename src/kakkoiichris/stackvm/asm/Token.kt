package stackvm.asm

data class Token(val row:Int, val col:Int, val type: TokenType) {
    fun toFloat()=
        type.value
}