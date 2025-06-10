package app.expressions

import app.expressions.values.*

abstract class EvaluatorBase {

    protected fun eval(expr: Expression): CellValue? = when (expr) {
        is CellValue -> expr
        is UnaryExpression -> evalUnary(expr)
        is BinaryExpression -> evalBinary(expr)
        is FunctionCallExpression -> evalFunction(expr)
        is CellRef -> evalCellRef(expr)
        else -> throw IllegalStateException("Unknown expression type: $expr")
    }

    private fun evalUnary(expr: UnaryExpression): CellValue {
        val v = eval(expr.arg) ?: IntVal(0)
        return when (expr.op) {
            OperatorType.MINUS -> v.toNumericValue().unaryMinus()
            OperatorType.PLUS -> v.toNumericValue().unaryPlus()
            OperatorType.NOT -> v.toBoolValue().not()
            else -> throw IllegalStateException("Unsupported unary operation: ${expr.op}")
        }
    }

    protected fun evalBinary(expr: BinaryExpression): CellValue {
        val l = eval(expr.left) ?: IntVal(0)
        val r = eval(expr.right) ?: IntVal(0)

        return when (expr.op) {
            OperatorType.PLUS -> l.toNumericValue().plus(r.toNumericValue())
            OperatorType.MINUS -> l.toNumericValue().minus(r.toNumericValue())
            OperatorType.MULTIPLY -> l.toNumericValue().times(r.toNumericValue())
            OperatorType.DIVIDE -> l.toNumericValue().div(r.toNumericValue())
            OperatorType.POWER -> l.toNumericValue().pow(r.toNumericValue())

            OperatorType.AND -> BoolVal(l.toBool() && r.toBool())
            OperatorType.OR -> BoolVal(l.toBool() || r.toBool())

            else -> throw IllegalStateException("Unsupported binary operation: ${expr.op}")
        }
    }

    protected fun evalFunction(expr: FunctionCallExpression): CellValue {
        val args = expr.args.mapNotNull { eval(it) }
        return when (expr.name.uppercase()) {
            "SUM" -> args.reduce { acc, it -> acc.toNumericValue().plus(it.toNumericValue()) }
            "AVG" -> args.reduce { acc, it -> acc.toNumericValue().plus(it.toNumericValue()) }
                .toNumericValue()
                .div(IntVal(args.count()))
            "ABS" -> {
                val n = args.first().toDouble()
                DoubleVal(kotlin.math.abs(n))
            }
            else -> throw IllegalStateException("Unsupported function: ${expr.name}")
        }
    }

    protected abstract fun evalCellRef(expr: CellRef): CellValue?
}
