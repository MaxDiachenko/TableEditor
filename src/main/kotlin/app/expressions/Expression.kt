package app.expressions

import app.ColumnNameHelper
import app.expressions.values.CellValue

interface Expression

data class UnaryExpression(val op: OperatorType, val arg: Expression) : Expression
data class BinaryExpression(val left: Expression, val op: OperatorType, val right: Expression) : Expression
data class FunctionCallExpression(val name: String, val args: List<Expression>) : Expression
data class CellRef(var row: Int, var col: Int): Expression

fun Expression.traverse(): Sequence<Expression> = sequence {
    yield(this@traverse)
    when (this@traverse) {
        is UnaryExpression -> yieldAll(arg.traverse())
        is BinaryExpression -> {
            yieldAll(left.traverse())
            yieldAll(right.traverse())
        }
        is FunctionCallExpression -> {
            for (arg in args) {
                yieldAll(arg.traverse())
            }
        }
    }
}

fun Expression.toFormulaString(): String = when (this) {
    is UnaryExpression -> "${opSymbol(op)}${arg.toFormulaString()}"
    is BinaryExpression -> "${left.toFormulaString()} ${opSymbol(op)} ${right.toFormulaString()}"
    is FunctionCallExpression -> "$name(${args.joinToString(",") { it.toFormulaString() }})"
    is CellRef -> "${ColumnNameHelper.toName(col)}${row + 1}"
    is CellValue -> this.toString()
    else -> throw IllegalArgumentException("Unknown Expression type: $this")
}

private fun opSymbol(op: OperatorType): String = when (op) {
    OperatorType.PLUS -> "+"
    OperatorType.MINUS -> "-"
    OperatorType.MULTIPLY -> "*"
    OperatorType.DIVIDE -> "/"
    OperatorType.POWER -> "^"
    OperatorType.AND -> "AND"
    OperatorType.OR -> "OR"
    OperatorType.NOT -> "NOT"
}