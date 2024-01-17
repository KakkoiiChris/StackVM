package kakkoiichris.stackvm.lang

import kakkoiichris.stackvm.asm.ASMToken

interface IASMToken {
    fun resolve(start: Float, end: Float): Ok

    class Ok(val token: ASMToken) : IASMToken {
        override fun resolve(start: Float, end: Float) =
            this
    }

    class AwaitStart(val offset: Int = 0) : IASMToken {
        override fun resolve(start: Float, end: Float) =
            Ok(ASMToken.Value(start + offset))
    }

    class AwaitEnd(val offset: Int = 0) : IASMToken {
        override fun resolve(start: Float, end: Float) =
            Ok(ASMToken.Value(end + offset))
    }
}

