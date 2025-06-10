package app.data

import app.expressions.CellRef
import app.expressions.values.CellValue

interface IDataModel {
    val cellCount: Int
    val rowCount: Int
    val columnCount: Int
    val formulas: Set<MetaFunc>
    fun getValue(ref: CellRef): CellValue?
    fun getEvaluatedValue(ref: CellRef): CellValue?
    fun getMeta(ref: CellRef): MetaCell?
    fun getOrCreateMeta(ref: CellRef): MetaCell
    fun setValue(ref: CellRef, cellValue: CellValue)
    fun setMeta(ref: CellRef, metaCell: MetaCell?)
    fun clear(ref: CellRef)
    fun removeColumn(colIndex: Int)
    fun removeRow(rowIndex: Int)
    fun insertRowAt(index: Int)
    fun insertColumnAt(index: Int)
    fun sortByColumn(colIndex: Int, descending: Boolean)
    fun canRemoveRow(rowIndex: Int): Boolean
    fun canRemoveColumn(colIndex: Int): Boolean

    fun copyTo(target: IDataModel) {
        for (row in 0 until rowCount) {
            for (col in 0 until columnCount) {
                val addr = CellRef(row, col)
                val value = getValue(addr)
                if (value != null) {
                    target.setValue(addr, value)
                    val meta = getMeta(addr)
                    if(meta != null){
                        target.setMeta(addr, meta)
                    }
                }
            }
        }
    }
}
