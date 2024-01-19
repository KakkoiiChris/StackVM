package kakkoiichris.stackvm.lang

import kakkoiichris.stackvm.asm.ASMToken

interface IASMToken {
    fun resolveStartAndEnd(start: Float, end: Float): Ok? = null

    fun resolveLast(last: Float): Ok? = null

    class Ok(val token: ASMToken) : IASMToken {
        override fun resolveStartAndEnd(start: Float, end: Float) = this

        override fun resolveLast(last: Float) = this
    }

    class AwaitStart(private val offset: Int = 0) : IASMToken {
        override fun resolveStartAndEnd(start: Float, end: Float) =
            Ok(ASMToken.Value(start + offset))
    }

    class AwaitEnd(private val offset: Int = 0) : IASMToken {
        override fun resolveStartAndEnd(start: Float, end: Float) =
            Ok(ASMToken.Value(end + offset))
    }

    class AwaitLast(private val offset: Int = 0) : IASMToken {
        override fun resolveLast(last: Float) =
            Ok(ASMToken.Value(last + offset))
    }
}

