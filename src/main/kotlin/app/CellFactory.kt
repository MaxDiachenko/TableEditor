package app

import app.data.*
import app.expressions.CellRef
import app.expressions.values.*

object CellFactory: ICellFactory {

    override fun fromString(input: String, address: CellRef): Pair<CellValue, MetaCell?> {
        val cell = CellValue.fromString(input)
        if (cell.toString().startsWith("=")) return Pair(cell, MetaFunc(address))
        return Pair(cell, null)
    }

    override fun fromAny(input: Any, address: CellRef): Pair<CellValue, MetaCell?> {
        val cell: CellValue = when (input) {
            is Int -> IntVal(input)
            is Double -> DoubleVal(input)
            is Boolean -> BoolVal(input)
            is java.time.LocalDateTime -> DateTimeVal(input)
            is String -> {
                val strValue = CellValue.fromString(input)
                return if (strValue.cellType() == CellType.FUNC) {
                    Pair(strValue, MetaFunc(address))
                } else {
                    Pair(strValue, null)
                }
            }
            is CellValue -> input
            else -> throw IllegalArgumentException("Unsupported value type: ${input::class}")
        }

        return Pair(cell, null)
    }
}
