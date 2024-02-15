package kakkoiichris.stackvm.lang.compiler

import kakkoiichris.stackvm.lang.parser.Node

interface Token {
    fun resolveStartAndEnd(start: Float, end: Float): Ok? = null

    fun resolveLabelStartAndEnd(label: Node.Name, start: Float, end: Float): Ok? = null

    fun resolveLast(last: Float): Ok? = null

    class Ok(val token: Bytecode) : Token {
        override fun resolveStartAndEnd(start: Float, end: Float) = this

        override fun resolveLast(last: Float) = this

        override fun toString() =
            "Ok<$token>"
    }

    class AwaitStart(private val offset: Int = 0) : Token {
        override fun resolveStartAndEnd(start: Float, end: Float) =
            Bytecode.Value(start + offset).intermediate

        override fun toString() =
            "AwaitStart<$offset>"
    }

    class AwaitEnd(private val offset: Int = 0) : Token {
        override fun resolveStartAndEnd(start: Float, end: Float) =
            Bytecode.Value(end + offset).intermediate

        override fun toString() =
            "AwaitEnd<$offset>"
    }

    class AwaitLast(private val offset: Int = 0) : Token {
        override fun resolveLast(last: Float) =
            Bytecode.Value(last + offset).intermediate

        override fun toString() =
            "AwaitLast<$offset>"
    }

    class AwaitLabelStart(private val label: Node.Name, private val offset: Int = 0) : Token {
        override fun resolveLabelStartAndEnd(label: Node.Name, start: Float, end: Float) =
            if (this.label.name.value == label.name.value)
                Bytecode.Value(start + offset).intermediate
            else
                null

        override fun toString() =
            "AwaitLabelStart<$offset, ${label.name.value}>"
    }

    class AwaitLabelEnd(private val label: Node.Name, private val offset: Int = 0) : Token {
        override fun resolveLabelStartAndEnd(label: Node.Name, start: Float, end: Float) =
            if (this.label.name.value == label.name.value)
                Bytecode.Value(end + offset).intermediate
            else
                null

        override fun toString() =
            "AwaitLabelEnd<$offset, ${label.name.value}>"
    }
}

