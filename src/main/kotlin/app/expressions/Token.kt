package app.expressions

enum class OperatorType {
    PLUS, MINUS, MULTIPLY, DIVIDE, POWER,
    AND, OR, NOT
}

interface Token


object EOFToken : Token
object LParenToken : Token
object RParenToken : Token
object CommaToken : Token
object ColonToken : Token

@JvmInline
value class FunctionToken(val name: String) : Token
@JvmInline
value class CellRefToken(val identifier: String) : Token
@JvmInline
value class OperatorToken(val op: OperatorType) : Token
