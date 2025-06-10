package app.expressions

import app.expressions.values.CellValue
import app.expressions.values.IntVal

object Optimizer: EvaluatorBase() {

    override fun evalCellRef(expr: CellRef): CellValue {
        throw IllegalStateException("CelRef evaluation during optimization expression: $expr")
    }

    fun optimize(expr: Expression): Expression {
        return when (expr) {
            is CellValue -> expr
            is CellRef -> expr

            is UnaryExpression -> {
                val optimizedExpr = optimize(expr.arg)
                if (containsCellRef(optimizedExpr)) {
                    UnaryExpression(expr.op, optimizedExpr)
                } else {
                    val folded = eval(UnaryExpression(expr.op, optimizedExpr))?:IntVal(0)
                    folded
                }
            }

            is BinaryExpression -> {
                val left = optimize(expr.left)
                val right = optimize(expr.right)

                if (containsCellRef(left) || containsCellRef(right)) {
                    BinaryExpression(left, expr.op, right)
                } else {
                    val folded = eval(BinaryExpression(left, expr.op, right))?:IntVal(0)
                    folded
                }
            }

            is FunctionCallExpression -> {
                val optimizedArgs = expr.args.map { optimize(it) }

                if (optimizedArgs.any { containsCellRef(it) }) {

                    FunctionCallExpression(expr.name, optimizedArgs)
                } else {
                    val folded = eval(FunctionCallExpression(expr.name, optimizedArgs))?:IntVal(0)
                    folded
                }
            }

            else -> throw IllegalArgumentException("Unknown expression type: $expr")
        }
    }

    private fun containsCellRef(expr: Expression): Boolean {
        return when (expr) {
            is CellRef -> true
            is UnaryExpression -> containsCellRef(expr.arg)
            is BinaryExpression -> containsCellRef(expr.left) || containsCellRef(expr.right)
            is FunctionCallExpression -> expr.args.any { containsCellRef(it) }
            is CellValue -> false
            else -> throw IllegalArgumentException("Unknown expression type: $expr")
        }
    }
}
