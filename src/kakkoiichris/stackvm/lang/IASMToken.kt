package kakkoiichris.stackvm.lang

import kakkoiichris.stackvm.asm.ASMToken

interface IASMToken {
    fun resolve(start: Float, end: Float): Ok

    class Ok(val token: ASMToken) : IASMToken {
        override fun resolve(start: Float, end: Float) =
            this
    }

    object AwaitStart : IASMToken {
        override fun resolve(start: Float, end: Float) =
            Ok(ASMToken.Value(start))
    }

    object AwaitEnd : IASMToken {
        override fun resolve(start: Float, end: Float) =
            Ok(ASMToken.Value(end))
    }
}

