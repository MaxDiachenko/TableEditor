package app

import app.data.MetaCell
import app.data.MetaFunc
import app.expressions.CellRef
import app.expressions.values.CellValue

interface ICellFactory {
    fun fromString(input: String, address: CellRef): Pair<CellValue, MetaCell?>
    fun fromAny(input: Any, address: CellRef): Pair<CellValue, MetaCell?>
}
