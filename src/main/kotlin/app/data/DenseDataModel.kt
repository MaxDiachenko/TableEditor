package app.data

import app.expressions.CellRef
import app.expressions.toFormulaString
import app.expressions.values.CellValue
import app.expressions.values.StringVal

class DenseDataModel(
    rowCount: Int,
    columnCount: Int
) : MetaDataModel(rowCount, columnCount), IDataModel {

    override var cellCount: Int = 0
        private set

    private var values: Array<Array<CellValue?>> = Array(rowCount) { Array(columnCount) { null } }

    override val formulas: Set<MetaFunc> get() = _formulas

    override fun getValue(ref: CellRef): CellValue? {
        return values[ref.row][ref.col]
    }

    override fun getEvaluatedValue(ref: CellRef): CellValue?{
        val meta = getMeta(ref)
        if(meta is MetaFunc)
            return meta.result ?: StringVal("Error")
        return getValue(ref)
    }

    override fun setValue(ref: CellRef, cellValue: CellValue) {
        if (values[ref.row][ref.col] == null) cellCount++
        values[ref.row][ref.col] = cellValue
    }

    override fun clear(ref: CellRef) {
        if (values[ref.row][ref.col] != null) {
            cellCount--
            values[ref.row][ref.col] = null
        }
        clearMetaFormula(ref)
    }

    override fun insertRowAt(index: Int) {
        values = values.toMutableList().apply {
            add(index, Array(columnCount) { null })
        }.toTypedArray()
        shiftMetaRowsRight(index)
        rebuildFormulas()
        rowCount++
    }

    override fun insertColumnAt(index: Int) {
        for (row in 0 until rowCount) {
            values[row] = values[row].toMutableList().apply {
                add(index, null)
            }.toTypedArray()
        }
        shiftMetaColumnsRight(index)
        rebuildFormulas()
        columnCount++
    }

    override fun removeRow(rowIndex: Int) {
        values = values.toMutableList().apply {
            removeAt(rowIndex)
        }.toTypedArray()
        shiftMetaRowsLeft(rowIndex)
        rebuildFormulas()
        rowCount--
    }

    override fun removeColumn(colIndex: Int) {
        for (row in 0 until rowCount) {
            values[row] = values[row].toMutableList().apply {
                removeAt(colIndex)
            }.toTypedArray()
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
        val indexedRows = values.mapIndexed { index, row ->
            index to (getEvaluatedValue(CellRef(index, colIndex)))
        }

        val sorted = if (descending) {
            indexedRows.sortedWith(compareBy(
                { it.second != null },
                { it.second?.toDouble() ?: 0.0 }
            )).reversed()
        } else {
            indexedRows.sortedWith(compareBy(
                { it.second == null },
                { it.second?.toDouble() ?: 0.0 }
            ))
        }

        val oldValues = values.copyOf()
        val rowMapping = IntArray(rowCount)
        for (newRow in 0 until rowCount) {
            val (originalRowIndex, rowValue) = sorted[newRow]
            values[newRow] = oldValues[originalRowIndex]
            rowMapping[originalRowIndex] = newRow
        }
        remapRows(rowMapping)
        rebuildFormulas()
    }
}
