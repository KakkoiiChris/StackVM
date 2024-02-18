package kakkoiichris.stackvm.lang.compiler

import kakkoiichris.stackvm.lang.parser.Node

interface Token {
    fun resolveStartAndEnd(start: Double, end: Double): Ok? = null

    fun resolveLabelStartAndEnd(label: Node.Name, start: Double, end: Double): Ok? = null

    fun resolveLast(last: Double): Ok? = null

    class Ok(val bytecode: Bytecode) : Token {
        override fun resolveStartAndEnd(start: Double, end: Double) = this

        override fun resolveLast(last: Double) = this

        override fun toString() =
            "Ok<$bytecode>"
    }

    class AwaitStart(private val offset: Int = 0) : Token {
        override fun resolveStartAndEnd(start: Double, end: Double) =
            Bytecode.Value(start + offset).ok

        override fun toString() =
            "AwaitStart<$offset>"
    }

    class AwaitEnd(private val offset: Int = 0) : Token {
        override fun resolveStartAndEnd(start: Double, end: Double) =
            Bytecode.Value(end + offset).ok

        override fun toString() =
            "AwaitEnd<$offset>"
    }

    class AwaitLast(private val offset: Int = 0) : Token {
        override fun resolveLast(last: Double) =
            Bytecode.Value(last + offset).ok

        override fun toString() =
            "AwaitLast<$offset>"
    }

    class AwaitLabelStart(private val label: Node.Name, private val offset: Int = 0) : Token {
        override fun resolveLabelStartAndEnd(label: Node.Name, start: Double, end: Double) =
            if (this.label.name.value == label.name.value)
                Bytecode.Value(start + offset).ok
            else
                null

        override fun toString() =
            "AwaitLabelStart<$offset, ${label.name.value}>"
    }

    class AwaitLabelEnd(private val label: Node.Name, private val offset: Int = 0) : Token {
        override fun resolveLabelStartAndEnd(label: Node.Name, start: Double, end: Double) =
            if (this.label.name.value == label.name.value)
                Bytecode.Value(end + offset).ok
            else
                null

        override fun toString() =
            "AwaitLabelEnd<$offset, ${label.name.value}>"
    }
}

