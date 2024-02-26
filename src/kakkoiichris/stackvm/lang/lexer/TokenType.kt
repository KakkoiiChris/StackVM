package kakkoiichris.stackvm.lang.lexer

import kakkoiichris.stackvm.lang.parser.DataType

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
        IMPORT,
        VOID,
        BOOL,
        INT,
        FLOAT,
        CHAR,
        ALIAS,
        STRUCT,
        ENUM;

        override fun toString() = name.lowercase()
    }

    enum class Symbol(private val rep: kotlin.String) : TokenType {
        PLUS("+"),

        PLUS_EQUAL("+=") {
            override val desugared = PLUS
        },

        DASH("-"),

        DASH_EQUAL("-=") {
            override val desugared = DASH
        },

        STAR("*"),

        STAR_EQUAL("*=") {
            override val desugared = STAR
        },

        SLASH("/"),

        SLASH_EQUAL("/=") {
            override val desugared = SLASH
        },

        PERCENT("%"),

        PERCENT_EQUAL("%=") {
            override val desugared = PERCENT
        },

        EQUAL("="),
        LESS("<"),
        LESS_EQUAL("<="),
        GREATER(">"),
        GREATER_EQUAL(">="),
        DOUBLE_AMPERSAND("&&"),

        DOUBLE_AMPERSAND_EQUAL("&&=") {
            override val desugared = DOUBLE_AMPERSAND
        },

        DOUBLE_PIPE("||"),

        DOUBLE_PIPE_EQUAL("||=") {
            override val desugared = DOUBLE_PIPE
        },

        DOUBLE_EQUAL("=="),
        EXCLAMATION("!"),
        EXCLAMATION_EQUAL("!="),
        LEFT_PAREN("("),
        RIGHT_PAREN(")"),
        LEFT_BRACE("{"),
        RIGHT_BRACE("}"),
        LEFT_SQUARE("["),
        RIGHT_SQUARE("]"),
        SEMICOLON(";"),
        COMMA(","),
        AT("@"),
        COLON(":"),
        POUND("#");

        open val desugared: Symbol? = null

        override fun toString() = rep
    }

    data class Value(val value: Double, val dataType: DataType) : TokenType {
        override fun toString() = value.toString()
    }

    data class String(val value: kotlin.String) : TokenType {
        override fun toString() =
            value
                .toCharArray()
                .joinToString(prefix = "\"", separator = "", postfix = "\"") {
                    if (it == '"') "\\\"" else "$it"
                }
    }

    data class Name(val value: kotlin.String) : TokenType {
        override fun toString() = value
    }

    data class Type(val value: DataType) : TokenType {
        override fun toString() = value.toString()
    }

    data object End : TokenType {
        override fun toString() = "EOF"
    }
}