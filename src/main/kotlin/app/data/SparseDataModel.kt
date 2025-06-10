package app.data

import app.expressions.CellRef
import app.expressions.toFormulaString
import app.expressions.traverse
import app.expressions.values.CellValue
import app.expressions.values.StringVal

class SparseDataModel(
    rowCount: Int,
    columnCount: Int
) : MetaDataModel(rowCount, columnCount), IDataModel {

    override var cellCount: Int = 0
        private set

    private var values: MutableMap<Int, MutableMap<Int, CellValue>> = mutableMapOf()

    override val formulas: Set<MetaFunc> get() = _formulas

    override fun getValue(ref: CellRef): CellValue? {
        return values[ref.row]?.get(ref.col)
    }

    override fun getEvaluatedValue(ref: CellRef): CellValue?{
        val meta = getMeta(ref)
        if(meta is MetaFunc)
            return meta.result ?: StringVal("Error")
        return getValue(ref)
    }

    override fun setValue(ref: CellRef, cellValue: CellValue) {
        val rowValues = values.getOrPut(ref.row) { mutableMapOf() }
        if (rowValues[ref.col] == null) cellCount++
        rowValues[ref.col] = cellValue
    }
    override fun clear(ref: CellRef) {
        values[ref.row]?.remove(ref.col)?.let {
            cellCount--
            if (values[ref.row]?.isEmpty() == true) values.remove(ref.row)
        }
        clearMetaFormula(ref)
    }

    override fun insertRowAt(index: Int) {
        val keysToShift = values.keys.filter { it >= index }.sortedDescending()
        for (key in keysToShift) {
            values[key + 1] = values.remove(key)!!
        }
        shiftMetaRowsRight(index)
        rebuildFormulas()
        rowCount++
    }

    override fun insertColumnAt(index: Int) {
        values.forEach { (_, rowMap) ->
            val colsToShift = rowMap.keys.filter { it >= index }.sortedDescending()
            for (col in colsToShift) {
                rowMap[col + 1] = rowMap.remove(col)!!
            }
        }

        shiftMetaColumnsRight(index)
        rebuildFormulas()
        columnCount++
    }

    override fun removeRow(rowIndex: Int) {
        values.remove(rowIndex)?.let { rowMap ->
            cellCount -= rowMap.size
        }

        val keysToShift = values.keys.filter { it > rowIndex }.sorted()
        for (key in keysToShift) {
            values[key - 1] = values.remove(key)!!
        }
        shiftMetaRowsLeft(rowIndex)
        rebuildFormulas()
        rowCount--
    }

    override fun removeColumn(colIndex: Int) {
        values.forEach { (row, rowMap) ->
            rowMap.remove(colIndex)?.let { cellCount-- }
            val colsToShift = rowMap.keys.filter { it > colIndex }.sorted()
            for (col in colsToShift) {
                rowMap[col - 1] = rowMap.remove(col)!!
            }
            if (rowMap.isEmpty()) values.remove(row)
        }
        shiftMetaColumnsLeft(colIndex)
        rebuildFormulas()
        columnCount--
    }

    private fun rebuildFormulas() {
        _formulas.forEach { func ->
            if (func.expression != null)
                setValue(func.address, StringVal('=' + func.expression!!.toFormulaString()))
        }
    }

    override fun sortByColumn(colIndex: Int, descending: Boolean) {
        val indexedRows = (0 until rowCount).map { rowIndex ->
            val value = getEvaluatedValue(CellRef(rowIndex, colIndex))
            rowIndex to value
        }

        val sorted = if (descending) {
            indexedRows.sortedWith(compareBy(
                { it.second == null },
                { -(it.second?.toDouble() ?: 0.0 )}
            ))
        } else {
            indexedRows.sortedWith(compareBy(
                { it.second == null },
                { it.second?.toDouble() ?: 0.0 }
            ))
        }

        val rowMapping = IntArray(rowCount)
        for (newRow in 0 until rowCount) {
            val (originalRowIndex, _) = sorted[newRow]
            rowMapping[originalRowIndex] = newRow
        }

        val oldValues = values.toMap()
        values.clear()

        for ((oldRow, rowMap) in oldValues) {
            val newRow = rowMapping[oldRow]
            values[newRow] = rowMap
        }

        remapRows(rowMapping)
        rebuildFormulas()
    }

}
