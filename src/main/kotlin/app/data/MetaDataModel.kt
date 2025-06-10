package app.data

import app.expressions.CellRef
import app.expressions.toFormulaString
import app.expressions.traverse

open class MetaDataModel(var rowCount: Int, var columnCount: Int) {
    private var meta: MutableMap<Int, MutableMap<Int, MetaCell>> = mutableMapOf()
    protected val _formulas = mutableSetOf<MetaFunc>()

    fun getMeta(ref: CellRef): MetaCell? {
        return meta[ref.row]?.get(ref.col)
    }

    fun getOrCreateMeta(ref: CellRef): MetaCell {
        var cell = getMeta(ref)
        if(cell == null){
            cell = MetaCell(mutableListOf())
            val rowMeta = meta.getOrPut(ref.row) { mutableMapOf() }
            rowMeta[ref.col] = cell
        }
        return cell
    }

    fun setMeta(ref: CellRef, metaCell: MetaCell?) {
        if(metaCell == null) {
            clearMetaFormula(ref)
            return
        }
        val rowMeta = meta.getOrPut(ref.row) { mutableMapOf() }
        val oldMeta = rowMeta[ref.col]
        if(oldMeta?.dependents?.isNotEmpty() == true)
            metaCell.dependents = rowMeta[ref.col]!!.dependents

        if (oldMeta is MetaFunc) {
            _formulas.remove(oldMeta)
            oldMeta.dependencies.forEach{d -> d.dependents.remove(oldMeta)}
        }

        rowMeta[ref.col] = metaCell
        if (metaCell is MetaFunc) _formulas.add(metaCell)
    }

    protected fun clearMetaFormula(ref: CellRef){
        val metaCell = meta[ref.row]?.get(ref.col)
        if(metaCell is MetaFunc)
        {
            _formulas.remove(metaCell)
            metaCell.dependencies.forEach{d -> d.dependents.remove(metaCell)}
            if(metaCell.dependents.any())
                meta[ref.row]!![ref.col] = MetaCell(metaCell.dependents)
            else
                meta[ref.row]?.remove(ref.col)
        }
    }

    fun shiftMetaRowsRight(index: Int){
        val maxRow = meta.keys.maxOrNull() ?: return
        for (row in maxRow downTo index) {
            val rowMap = meta.remove(row)
            if (!rowMap.isNullOrEmpty()) {
                meta[row + 1] = rowMap
            }
        }

        _formulas.forEach { func ->
            if (func.address.row >= index) func.address.row++
            func.expression?.traverse()?.filterIsInstance<CellRef>()?.forEach { r -> if (r.row >= index) r.row++ }
        }
    }

    fun shiftMetaRowsLeft(index: Int){
        val maxRow = meta.keys.maxOrNull() ?: return
        for (row in (index + 1)..maxRow) {
            val rowMap = meta.remove(row)
            if (!rowMap.isNullOrEmpty()) {
                meta[row - 1] = rowMap
            }
        }

        _formulas.forEach { func ->
            if (func.address.row > index) func.address.row--
            func.expression?.traverse()?.filterIsInstance<CellRef>()?.forEach { r -> if (r.row > index) r.row-- }
        }
    }

    fun shiftMetaColumnsRight(index: Int){
        meta.values.forEach { rowMap ->
            val maxCol = rowMap.keys.maxOrNull() ?: return@forEach
            for (col in maxCol downTo index) {
                rowMap.remove(col)?.let { metaCell ->
                    rowMap[col + 1] = metaCell
                }
            }
        }

        _formulas.forEach { func ->
            if (func.address.col >= index) func.address.col++
            func.expression?.traverse()?.filterIsInstance<CellRef>()?.forEach { r -> if (r.col >= index) r.col++ }
        }
    }

    fun shiftMetaColumnsLeft(index: Int){
        meta.values.forEach { rowMap ->
            val maxCol = rowMap.keys.maxOrNull() ?: return@forEach
            for (col in (index + 1)..maxCol) {
                rowMap.remove(col)?.let { metaCell ->
                    rowMap[col - 1] = metaCell
                }
            }
        }

        _formulas.forEach { func ->
            if (func.address.col > index) func.address.col--
            func.expression?.traverse()?.filterIsInstance<CellRef>()?.forEach { r -> if (r.col > index) r.col-- }
        }
    }

    fun canRemoveRow(rowIndex: Int): Boolean {
        return (0 until columnCount).none { col ->
            meta[rowIndex]?.get(col)?.dependents?.isNotEmpty() == true
        }
    }

    fun canRemoveColumn(colIndex: Int): Boolean {
        return (0 until rowCount).none { row ->
            meta[row]?.get(colIndex)?.dependents?.isNotEmpty() == true
        }
    }

    fun remapRows(rowMapping: IntArray){
        val oldMeta = meta.toMap()
        meta.clear()

        oldMeta.forEach { (oldRow, rowMap) ->
            val newRow = rowMapping[oldRow]
            meta[newRow] = rowMap
        }

        _formulas.forEach { func ->
            func.address.row = rowMapping[func.address.row]
            func.expression?.traverse()?.filterIsInstance<CellRef>()?.forEach { r -> r.row = rowMapping[r.row] }
        }
    }
}
