package kakkoiichris.stackvm.lang

interface TokenType {
    enum class Keyword : TokenType {
        VAR,
        IF,
        ELSE,
        WHILE,
        BREAK,
        CONTINUE,
        FUNCTION,
        RETURN
    }

    enum class Symbol : TokenType {
        PLUS,
        DASH,
        STAR,
        SLASH,
        PERCENT,
        EQUAL,
        LESS,
        LESS_EQUAL,
        GREATER,
        GREATER_EQUAL,
        DOUBLE_AMPERSAND,
        DOUBLE_PIPE,
        DOUBLE_EQUAL,
        EXCLAMATION,
        EXCLAMATION_EQUAL,
        LEFT_PAREN,
        RIGHT_PAREN,
        LEFT_BRACE,
        RIGHT_BRACE,
        SEMICOLON,
        COMMA,
        BACK_SLASH
    }

    data class Value(val value: Float) : TokenType

    data class Name(val value: String) : TokenType

    data object End : TokenType
}