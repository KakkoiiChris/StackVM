package kakkoiichris.stackvm.compiler

import kakkoiichris.stackvm.asm.ASMToken
import kakkoiichris.stackvm.lang.Node

interface IASMToken {
    fun resolveStartAndEnd(start: Float, end: Float): Ok? = null

    fun resolveLabelStartAndEnd(label: Node.Name, start: Float, end: Float): Ok? = null

    fun resolveLast(last: Float): Ok? = null

    class Ok(val token: ASMToken) : IASMToken {
        override fun resolveStartAndEnd(start: Float, end: Float) = this

        override fun resolveLast(last: Float) = this

        override fun toString() =
            "Ok<$token>"
    }

    class AwaitStart(private val offset: Int = 0) : IASMToken {
        override fun resolveStartAndEnd(start: Float, end: Float) =
            Ok(ASMToken.Value(start + offset))

        override fun toString() =
            "AwaitStart<$offset>"
    }

    class AwaitEnd(private val offset: Int = 0) : IASMToken {
        override fun resolveStartAndEnd(start: Float, end: Float) =
            Ok(ASMToken.Value(end + offset))

        override fun toString() =
            "AwaitEnd<$offset>"
    }

    class AwaitLast(private val offset: Int = 0) : IASMToken {
        override fun resolveLast(last: Float) =
            Ok(ASMToken.Value(last + offset))

        override fun toString() =
            "AwaitLast<$offset>"
    }

    class AwaitLabelStart(private val label: Node.Name, private val offset: Int = 0) : IASMToken {
        override fun resolveLabelStartAndEnd(label: Node.Name, start: Float, end: Float) =
            if (this.label.name.value == label.name.value)
                Ok(ASMToken.Value(start + offset))
            else
                null

        override fun toString() =
            "AwaitLabelStart<$offset, ${label.name.value}>"
    }

    class AwaitLabelEnd(private val label: Node.Name, private val offset: Int = 0) : IASMToken {
        override fun resolveLabelStartAndEnd(label: Node.Name, start: Float, end: Float) =
            if (this.label.name.value == label.name.value)
                Ok(ASMToken.Value(end + offset))
            else
                null

        override fun toString() =
            "AwaitLabelEnd<$offset, ${label.name.value}>"
    }
}

