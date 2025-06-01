/*   ______  ____   ____  ____    ____  _____
 * .' ____ \|_  _| |_  _||_   \  /   _||_   _|
 * | (___ \_| \ \   / /    |   \/   |    | |
 *  _.____`.   \ \ / /     | |\  /| |    | |   _
 * | \____) |   \ ' /     _| |_\/_| |_  _| |__/ |
 *  \______.'    \_/     |_____||_____||________|
 *
 *         Stack Virtual Machine Language
 *     Copyright (C) 2024 Christian Alexander
 */
package kakkoiichris.svml.lang.compiler

import kakkoiichris.svml.lang.parser.Node

interface Token {
    fun resolveStartAndEnd(start: Double, end: Double): Ok? = null

    fun resolveLabelStartAndEnd(label: Node.Name, start: Double, end: Double): Ok? = null

    fun resolveLast(last: Double): Ok? = null

    fun resolveFunction(id: Int, start: Double): Ok? = null

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
            if (this.label.value == label.value)
                Bytecode.Value(start + offset).ok
            else
                null

        override fun toString() =
            "AwaitLabelStart<$offset, ${label.value}>"
    }

    class AwaitLabelEnd(private val label: Node.Name, private val offset: Int = 0) : Token {
        override fun resolveLabelStartAndEnd(label: Node.Name, start: Double, end: Double) =
            if (this.label.value == label.value)
                Bytecode.Value(end + offset).ok
            else
                null

        override fun toString() =
            "AwaitLabelEnd<$offset, ${label.value}>"
    }

    class AwaitFunction(private val id: Int, private val offset: Int = 0) : Token {
        override fun resolveFunction(id: Int, start: Double): Ok? =
            if (this.id == id)
                Bytecode.Value(start).ok
            else
                null

        override fun toString() =
            "AwaitFunction<$offset, $id>"
    }
}