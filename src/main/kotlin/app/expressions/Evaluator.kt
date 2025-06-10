package app.expressions

import app.data.IDataModel
import app.data.MetaFunc
import app.expressions.values.*

class Evaluator(val data: IDataModel): IEvaluator, EvaluatorBase() {

    override fun evaluate(expression: Expression): CellValue {
        try {
            return eval(expression)?: IntVal(0)
        }catch(e: Exception){
            throw IllegalArgumentException(e.message)
        }
    }

    override fun evalCellRef(expr: CellRef): CellValue? {
        val meta = data.getMeta(expr)
        if(meta is MetaFunc)
        {
            return meta.result ?:  throw IllegalArgumentException("Dependency has an error.")
        }
        return data.getValue(expr)
    }
}
