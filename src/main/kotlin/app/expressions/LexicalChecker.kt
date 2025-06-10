package app.expressions

import app.expressions.values.CellValue

class LexicalChecker {
    private var parenLevel = 0
    fun checkSyntaxIncremental(tokens: MutableList<Token>) {
        val current = tokens.lastOrNull() ?: return
        val index = tokens.lastIndex
        val prev = tokens.getOrNull(index - 1)

        if(prev is ColonToken && current !is CellRefToken)
            throw IllegalArgumentException("Unexpected token at position $index")

        when (current) {
            is LParenToken -> parenLevel++
            is RParenToken -> {
                parenLevel--
                if (parenLevel < 0) {
                    throw IllegalArgumentException("Unexpected ')' at position $index")
                }
            }
            is CommaToken -> {
                if (parenLevel <= 0 || prev is OperatorToken) {
                    throw IllegalArgumentException("Unexpected ',' at position $index")
                }
            }
            is ColonToken -> {
                if(prev !is CellRefToken) {
                    throw IllegalArgumentException("Unexpected ':' at position $index")
                }
            }
        }

        val isUnaryContext = prev == null
                || prev is OperatorToken
                || prev === LParenToken
                || prev === CommaToken

        if (current is OperatorToken) {
            checkOperationContext(current, isUnaryContext, prev, index)
        }

        if (current is EOFToken){
            checkFinal(prev)
        }
    }

    private fun checkOperationContext(
        current: OperatorToken,
        isUnaryContext: Boolean,
        prev: Token?,
        index: Int
    ) {
        when (current.op) {
            OperatorType.PLUS, OperatorType.MINUS -> {
                if (!isUnaryContext) {
                    if (prev !is CellValue && prev !is CellRefToken && prev !== RParenToken) {
                        throw IllegalArgumentException("Unexpected binary operator '${current.op}' at position $index")
                    }
                }
            }

            OperatorType.NOT -> {
                if (!isUnaryContext) {
                    throw IllegalArgumentException("Unexpected unary operator '${current.op}' at position $index")
                }
            }

            else -> {
                if (prev == null || prev is OperatorToken || prev === LParenToken) {
                    throw IllegalArgumentException("Unexpected binary operator '${current.op}' at position $index")
                }
            }
        }
    }

    private fun checkFinal(prev: Token?) {
        if (parenLevel != 0) {
            throw IllegalArgumentException("')' expected")
        }

        if (prev is OperatorToken) {
            throw IllegalArgumentException("Expression cannot end with operator '${prev.op}'")
        }
    }
}