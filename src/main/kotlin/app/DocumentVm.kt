package app

import app.data.*
import app.expressions.*
import app.expressions.values.CellValue
import org.koin.core.component.*

class DocumentVm(
    private val cellFactory: ICellFactory,
    private val denseDataModelFactory: (Int, Int) -> IDataModel,
    private val sparseDataModelFactory: (Int, Int) -> IDataModel,
    private val evaluatorFactory: (IDataModel) -> IEvaluator,
    private var model: IDataModel,
    private val _isSparseMode: Boolean
) : IDocumentVm, KoinComponent {
    private var hasChanges: Boolean = false
    private var isSparseMode: Boolean = false
    private var evaluator: IEvaluator

    override val rowCount: Int
        get() = model.rowCount
    override val columnCount: Int
        get() = model.columnCount

    override val data: IDataModel
        get() = model

    init {
        isSparseMode = _isSparseMode
        evaluator = evaluatorFactory(model)
        if(model.cellCount > 0){
            DependencyBuilder.buildAllDependencies(model).forEach{f ->
                f.result = evaluator.evaluate(f.expression!!)}
        }
    }

    override fun hasUnsavedChanges(): Boolean {
        return hasChanges
    }

    override fun setValue(row: Int, col: Int, newValue: Any?):Sequence<CellRef> = sequence {
        hasChanges = true
        val ref = CellRef(row, col)

        if(newValue == null || newValue == "") {
            model.clear(ref)
            yieldAll(onCellChanged(ref))
            return@sequence
        }

        val (cellValue, metaCell) = cellFactory.fromAny(newValue, ref)

        model.setValue(ref, cellValue)
        model.setMeta(ref, metaCell)

        if(metaCell is MetaFunc) {
            try {
                processFormula(metaCell, cellValue)
            } catch (e: Exception) {
                metaCell.result = null
                metaCell.error = e.message
                throw e
            }finally {
                yieldAll(onCellChanged(ref))
            }
            return@sequence
        }
        yieldAll(onCellChanged(ref))
    }

    private fun processFormula(metaCell: MetaFunc, cellValue: CellValue) {
        metaCell.expression = Parser.parse(cellValue.toString())
        metaCell.expression!!.traverse().filterIsInstance<CellRef>()
            .forEach { r -> metaCell.dependencies.add(model.getOrCreateMeta(r)) }
        DependencyBuilder.updateCellDependencies(metaCell)
        metaCell.result = evaluator.evaluate(metaCell.expression!!)
        metaCell.error = null
    }

    private fun onCellChanged(ref: CellRef): Sequence<CellRef> = sequence{
        checkSwitchDataModel()
        val metaCell = model.getMeta(ref)
        if (metaCell != null)
            yieldAll(updateDependents(metaCell))
        yield(ref)
    }

    private fun updateDependents(cell: MetaCell): Sequence<CellRef> = sequence {
        cell.dependents.forEach{
            dep ->
            dep.result = null;
            try {
                dep.result = evaluator.evaluate(dep.expression!!)
                dep.error = null
            }
            catch(e: IllegalArgumentException){
                dep.error = e.message
            }
            yield(dep.address)
            yieldAll(updateDependents(dep))
        }
    }

    override fun getVisualValue(row: Int, col: Int): Any? = model.getEvaluatedValue(CellRef(row, col))

    override fun getEditValue(row: Int, col: Int): Any? =
        model.getValue(CellRef(row, col))

    override fun addRowAfter(rowIndex: Int) {
        model.insertRowAt(rowIndex + 1)
    }

    override fun addColumnAfter(colIndex: Int) {
        model.insertColumnAt(colIndex + 1)
    }

    override fun addRowBefore(rowIndex: Int) {
        model.insertRowAt(rowIndex)
    }

    override fun addColumnBefore(colIndex: Int) {
        model.insertColumnAt(colIndex)
    }

    override fun removeRow(rowIndex: Int) {
        if(!model.canRemoveRow(rowIndex))
            throw IllegalArgumentException("Cells used in expressions can't be removed")
        model.removeRow(rowIndex)
    }

    override fun removeColumn(colIndex: Int) {
        if(!model.canRemoveColumn(colIndex))
            throw IllegalArgumentException("Cells used in expressions can't be removed")
        model.removeColumn(colIndex)
    }

    override fun sortAscending(colIndex: Int) {
        model.sortByColumn(colIndex, false)
    }

    override fun sortDescending(colIndex: Int) {
        model.sortByColumn(colIndex, true)
    }

    override fun getPresentationType(row: Int, col: Int): CellType? {
        val ref = CellRef(row, col)
        val meta = model.getMeta(ref)
        if(meta is MetaFunc)
            return meta.result?.cellType()
        return model.getValue(ref)?.cellType()
    }

    override fun getType(row: Int, col: Int): CellType? {
        val ref = CellRef(row, col)
        return model.getValue(ref)?.cellType()
    }

    override fun getCellError(row: Int, col: Int): String? {
        val ref = CellRef(row, col)
        val meta = model.getMeta(ref)
        if(meta is MetaFunc && meta.error != null)
            return meta.error
        return null
    }

    private fun checkSwitchDataModel() {
        if (isSparseMode && model.cellCount > (model.rowCount * model.columnCount) * 0.4) {
            val newModel = denseDataModelFactory(model.rowCount, model.columnCount)
            model.copyTo(newModel)
            isSparseMode = false
            model = newModel
            evaluator = evaluatorFactory(model)
            return
        }
        if (!isSparseMode && model.cellCount < (model.rowCount * model.columnCount) * 0.3) {
            val newModel = sparseDataModelFactory(model.rowCount, model.columnCount)
            model.copyTo(newModel)
            isSparseMode = true
            model = newModel
            evaluator = evaluatorFactory(model)
            return
        }
    }
}