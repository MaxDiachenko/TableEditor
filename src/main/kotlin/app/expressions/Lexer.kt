package app.expressions

import app.expressions.values.*

class Lexer(private val input: String) {
    private var pos = 0

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        val checker = LexicalChecker()
        while (true) {
            val token = nextToken()
            tokens.add(token)
            checker.checkSyntaxIncremental(tokens)
            if (token === EOFToken) break
        }
        return tokens
    }

    fun nextToken(): Token {
        skipWhitespace()
        if (pos >= input.length) return EOFToken

        return when (val ch = input[pos]) {
            in '0'..'9', '.' -> readNumber()
            in 'A'..'Z', in 'a'..'z' -> readIdentifier()
            '+' -> { pos++; OperatorToken(OperatorType.PLUS) }
            '-' -> { pos++; OperatorToken(OperatorType.MINUS) }
            '*' -> { pos++; OperatorToken(OperatorType.MULTIPLY) }
            '/' -> { pos++; OperatorToken(OperatorType.DIVIDE) }
            '^' -> { pos++; OperatorToken(OperatorType.POWER) }
            ':' -> { pos++; ColonToken }
            '(' -> { pos++; LParenToken }
            ')' -> { pos++; RParenToken }
            ',' -> { pos++; CommaToken }
            else -> throw IllegalArgumentException("Invalid char: $ch")
        }
    }

    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    private fun readNumber(): CellValue {
        val start = pos
        var hasDot = false
        while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
            if (input[pos] == '.') hasDot = true
            pos++
        }
        val value = input.substring(start, pos)
        return if (hasDot) DoubleVal(value.toDouble()) else IntVal(value.toInt())
    }

    private fun readIdentifier(): Token {
        val start = pos
        while (pos < input.length && (input[pos].isLetterOrDigit())) pos++
        return when (val text = input.substring(start, pos).uppercase()) {
            "AND" -> OperatorToken(OperatorType.AND)
            "OR" -> OperatorToken(OperatorType.OR)
            "NOT" -> OperatorToken(OperatorType.NOT)
            "TRUE" -> BoolVal(true)
            "FALSE" -> BoolVal(false)
            "SUM", "AVG", "ABS" -> FunctionToken(text)
            else -> CellRefToken(text)
        }
    }
}

