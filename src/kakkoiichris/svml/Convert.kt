package kakkoiichris.svml

import kakkoiichris.svml.lang.Allocator
import kakkoiichris.svml.lang.Semantics
import kakkoiichris.svml.lang.Source
import kakkoiichris.svml.lang.compiler.Compiler
import kakkoiichris.svml.lang.lexer.Lexer
import kakkoiichris.svml.lang.parser.Parser
import kakkoiichris.svml.linker.Linker
import java.io.File

fun convert(srcFile: File): DoubleArray {
    val source = Source.of(srcFile)

    val lexer = Lexer(source)

    Linker.link()

    val parser = Parser(lexer)

    val program = parser.parse()

    Semantics.check(program)

    Allocator.allocate(program)

    val compiler = Compiler(program, optimize = true, generateComments = false)

    return compiler.compile()
}