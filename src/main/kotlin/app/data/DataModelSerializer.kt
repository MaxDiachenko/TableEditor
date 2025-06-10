package app.data

import app.ICellFactory
import app.expressions.CellRef
import app.expressions.Parser
import app.expressions.traverse
import app.expressions.values.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

object DataModelSerializer: KoinComponent, IDataModelWriter, IDataModelReader {
    private val cellFactory: ICellFactory by inject()
    private val mapper = ObjectMapper().registerModule(KotlinModule())

    override fun write(model: IDataModel, file: File) {
        val storedCells = mutableListOf<StoredCell>()

        for (row in 0 until model.rowCount) {
            for (col in 0 until model.columnCount) {
                val value = model.getValue(CellRef(row, col)) ?: continue
                val valueString = serializeValue(value)
                storedCells.add(StoredCell(row, col, valueString))
            }
        }

        val data = StoredData(model.rowCount, model.columnCount, storedCells)
        mapper.writeValue(file, data)
    }

    override fun read(file: File, factory: (rows: Int, cols: Int) -> IDataModel): IDataModel {
        val data: StoredData = mapper.readValue(file)
        val model = factory(data.rowCount, data.columnCount)

        for (stored in data.cells) {
            val ref = CellRef(stored.row, stored.col)
            val (value, meta) = deserializeValue(stored)
            model.setValue(ref, value)
            if(meta != null) {
                model.setMeta(ref, meta)
                if(meta is MetaFunc) meta.expression = Parser.parse(value.toString())
            }
        }

        model.formulas.forEach { f ->
            f.dependencies = f.expression!!.traverse().filterIsInstance<CellRef>()
                .map { r ->
                    model.getOrCreateMeta(r).also { it.dependents.add(f) }
                }.toMutableList()
        }
        return model
    }

    private fun serializeValue(cellValue: CellValue): String = cellValue.toString()
    private fun deserializeValue(stored: StoredCell): Pair<CellValue, MetaCell?> = cellFactory.fromString(stored.value, CellRef(stored.row, stored.col))

    private data class StoredData(val rowCount: Int, val columnCount: Int, val cells: List<StoredCell>)
    private data class StoredCell(val row: Int, val col: Int, val value: String)
}
