package app.expressions

import app.data.MetaFunc
import app.expressions.values.CellValue

interface IEvaluator {
    fun evaluate(expression: Expression): CellValue
}
