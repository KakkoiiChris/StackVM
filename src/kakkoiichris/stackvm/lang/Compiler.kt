package kakkoiichris.stackvm.lang

object Compiler {
    fun compile(src: String): List<Float> {
        val lexer = Lexer(src)

        val parser = Parser(lexer, false)

        val converter = ASMConverter(parser, false)

        val tokens = converter.convert()

        return tokens.map { it.value }
    }
}