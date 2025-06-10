package app.expressions

import app.ColumnNameHelper
import app.expressions.values.CellValue

object Parser {

    private val precedence = mapOf(
        OperatorType.OR to 1,
        OperatorType.AND to 2,
        OperatorType.PLUS to 3,
        OperatorType.MINUS to 3,
        OperatorType.MULTIPLY to 4,
        OperatorType.DIVIDE to 4,
        OperatorType.POWER to 5
    )

    fun parse(formula: String): Expression {
        val lexer = Lexer(formula.drop(1))
        val tokens = lexer.tokenize()
        val parserContext = ParserContext(tokens)
        val expr = parserContext.parseExpression()
        return Optimizer.optimize(expr)
    }

    private class ParserContext(private val tokens: List<Token>) {
        private var pos = 0

        private fun current(): Token = if (pos < tokens.size) tokens[pos] else EOFToken
        private fun next(): Token = if (pos + 1 < tokens.size) tokens[pos + 1] else EOFToken
        private fun advance() = tokens.getOrNull(pos++) ?: EOFToken
        private fun stepBack() = tokens.getOrNull(--pos)

        fun parseExpression(minPrecedence: Int = 1): Expression {
            var left = parseUnary()

            while (true) {
                val token = current()
                if (token !is OperatorToken) break

                val prec = precedence[token.op] ?: break
                if (prec < minPrecedence) break
                advance()

                var right = parseUnary()
                while (true) {
                    val nextOperator = current()
                    if (nextOperator is OperatorToken) {
                        val nextPrec = precedence[nextOperator.op] ?: break
                        if (nextPrec > prec) {
                            stepBack()
                            right = parseExpression(nextPrec)
                        } else break
                    } else break
                }

                left = BinaryExpression(left, token.op, right)
            }

            return left
        }

        private fun parseUnary(): Expression {
            val token = current()
            if (token is OperatorToken && token.op in listOf(OperatorType.PLUS, OperatorType.MINUS, OperatorType.NOT)) {
                advance()
                return UnaryExpression(token.op, parseUnary())
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Expression {
            return when (val token = current()) {
                is CellValue -> { advance(); token }
                is FunctionToken -> {
                    advance()
                    if (current() === LParenToken) {
                        advance()
                        val args = mutableListOf<Expression>()
                        if (current() !== RParenToken) {
                            do {
                                val curr = current()
                                if (curr is CellRefToken && next() == ColonToken) {
                                    advance()
                                    advance()
                                    val end = advance()
                                    if (end !is CellRefToken) {
                                        throw IllegalArgumentException("Unexpected identifier: $end")
                                    }
                                    args.addAll(parseCelRefRange(curr, end))
                                }
                                else
                                    args.add(parseExpression())
                            } while (current() === CommaToken && { advance(); true }())
                        }
                        if (current() !== RParenToken) throw IllegalStateException("Expected )")
                        advance()
                        FunctionCallExpression(token.name, args)
                    } else {
                        throw IllegalStateException("Expected (")
                    }
                }
                is LParenToken -> {
                    advance()
                    val expr = parseExpression()
                    if (current() !== RParenToken) throw IllegalStateException("Expected )")
                    advance()
                    expr
                }
                is CellRefToken -> {
                    if (next() == ColonToken) {
                        throw IllegalArgumentException("Unexpected identifier: ${token.identifier}")
                    }
                    advance()
                    parseCelRef(token.identifier)
                }
                else -> throw IllegalArgumentException("Unexpected token: $token")
            }
        }

        private fun parseCelRefRange(first: CellRefToken, last: CellRefToken): Sequence<CellRef> = sequence {
            val firstRef = parseCelRef(first.identifier)
            val lastRef = parseCelRef(last.identifier)

            val startCol = minOf(firstRef.col, lastRef.col)
            val endCol = maxOf(firstRef.col, lastRef.col)

            val startRow = minOf(firstRef.row, lastRef.row)
            val endRow = maxOf(firstRef.row, lastRef.row)

            for (col in startCol..endCol) {
                for (row in startRow..endRow) {
                    yield(CellRef(row, col))
                }
            }
        }

        private fun parseCelRef(identifier: String): CellRef {
            var i = 0
            val n = identifier.length

            while (i < n && identifier[i].isLetter()) {
                i++
            }

            if (i == 0 || i == n) {
                throw IllegalArgumentException("Unexpected identifier: $identifier")
            }

            val col = identifier.substring(0, i).uppercase()
            val numberPart = identifier.substring(i)

            val row = numberPart.toIntOrNull()
                ?: throw IllegalArgumentException("Unexpected identifier: $identifier")

            return CellRef(row - 1, ColumnNameHelper.toIndex(col))
        }
    }
}
