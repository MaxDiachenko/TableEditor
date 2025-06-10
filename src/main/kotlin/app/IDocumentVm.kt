package app

import app.data.CellType
import app.data.IDataModel
import app.expressions.CellRef

interface IDocumentVm {
    val rowCount: Int
    val columnCount: Int
    val data: IDataModel
    fun setValue(row: Int, col: Int, newValue: Any?): Sequence<CellRef>
    fun getVisualValue(row: Int, col: Int): Any?
    fun getEditValue(row: Int, col: Int): Any?
    fun addRowAfter(rowIndex: Int): Unit
    fun addColumnAfter(colIndex: Int): Unit
    fun addRowBefore(rowIndex: Int): Unit
    fun addColumnBefore(colIndex: Int): Unit
    fun removeRow(rowIndex: Int): Unit
    fun removeColumn(colIndex: Int): Unit
    fun getPresentationType(row: Int, col: Int): CellType?
    fun hasUnsavedChanges(): Boolean
    fun sortAscending(colIndex: Int)
    fun sortDescending(colIndex: Int)
    fun getType(row: Int, col: Int): CellType?
    fun getCellError(row: Int, col: Int): String?
}
