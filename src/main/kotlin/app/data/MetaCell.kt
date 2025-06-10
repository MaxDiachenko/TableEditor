package app.data

import app.expressions.CellRef
import app.expressions.Expression
import app.expressions.Parser
import app.expressions.values.CellValue

enum class CellType {
    STRING, INT, BOOL, DOUBLE, DATETIME, FUNC
}

open class MetaCell(
    var dependents: MutableList<MetaFunc>
)

class MetaFunc(
    val address: CellRef
) : MetaCell(mutableListOf()){
    var error: String? = null
    var dependencies: MutableList<MetaCell> = mutableListOf()
    var expression: Expression? = null
    var result: CellValue? = null
}
