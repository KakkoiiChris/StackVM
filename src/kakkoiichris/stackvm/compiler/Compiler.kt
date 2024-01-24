package kakkoiichris.stackvm.compiler

import kakkoiichris.stackvm.lang.Lexer
import kakkoiichris.stackvm.lang.Parser

object Compiler {
    fun compile(src: String): FloatArray {
        val lexer = Lexer(src)

        val parser = Parser(lexer, false)

        val converter = ASMConverter(parser, false)

        val tokens = converter.convert()

        return FloatArray(tokens.size) { i -> tokens[i].value }
    }
}