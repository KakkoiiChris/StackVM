package kakkoiichris.stackvm.lang

interface TokenType {
    enum class Keyword : TokenType {
        LET,
        VAR,
        IF,
        ELSE,
        WHILE,
        DO,
        FOR,
        BREAK,
        CONTINUE,
        FUNCTION,
        RETURN,
        VOID,
        BOOL,
        INT,
        FLOAT,
        CHAR
    }

    enum class Symbol : TokenType {
        PLUS,
        PLUS_EQUAL {
            override val desugared = PLUS
        },
        DASH,
        DASH_EQUAL {
            override val desugared = DASH
        },
        STAR,
        STAR_EQUAL {
            override val desugared = STAR
        },
        SLASH,
        SLASH_EQUAL {
            override val desugared = SLASH
        },
        PERCENT,
        PERCENT_EQUAL {
            override val desugared = PERCENT
        },
        EQUAL,
        LESS,
        LESS_EQUAL,
        GREATER,
        GREATER_EQUAL,
        DOUBLE_AMPERSAND,
        DOUBLE_AMPERSAND_EQUAL {
            override val desugared = DOUBLE_AMPERSAND
        },
        DOUBLE_PIPE,
        DOUBLE_PIPE_EQUAL {
            override val desugared = DOUBLE_PIPE
        },
        DOUBLE_EQUAL,
        EXCLAMATION,
        EXCLAMATION_EQUAL,
        LEFT_PAREN,
        RIGHT_PAREN,
        LEFT_BRACE,
        RIGHT_BRACE,
        LEFT_SQUARE,
        RIGHT_SQUARE,
        SEMICOLON,
        COMMA,
        AT,
        COLON;

        open val desugared: Symbol? = null
    }

    data class Value(val value: Float, val dataType: DataType) : TokenType

    data class String(val value: kotlin.String) : TokenType

    data class Name(val value: kotlin.String) : TokenType

    data class Type(val value: DataType) : TokenType

    data object End : TokenType
}