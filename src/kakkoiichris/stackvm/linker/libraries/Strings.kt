package kakkoiichris.stackvm.linker.libraries

import kakkoiichris.stackvm.lang.lexer.Location
import kakkoiichris.stackvm.lang.lexer.TokenType
import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.lang.parser.Node
import kakkoiichris.stackvm.linker.Link
import kakkoiichris.stackvm.linker.Linker

object Strings : Link {
    override val name = "strings"

    override fun open(linker: Linker) {
    }

    override fun close(linker: Linker) {
        TODO("Not yet implemented")
    }
}