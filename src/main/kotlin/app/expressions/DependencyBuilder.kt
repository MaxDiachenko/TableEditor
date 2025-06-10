package app.expressions

import app.data.IDataModel
import app.data.MetaCell
import app.data.MetaFunc

object DependencyBuilder {

    fun buildAllDependencies(model: IDataModel): List<MetaFunc> {
        val roots = model.formulas.toMutableSet()
        for (row in 0 until model.rowCount) {
            for (col in 0 until model.columnCount) {
                val meta = model.getMeta(CellRef(row, col))
                if (meta != null && meta.dependents.isNotEmpty()) {
                    val depSet = meta.dependents.toSet()
                    if(meta is MetaFunc)
                        roots.removeAll(depSet)
                    meta.dependents = reorderForUpdate(depSet)
                }
            }
        }
        return reorderForUpdate(roots)
    }

    fun updateCellDependencies(cell: MetaFunc){
        updateDependents(cell)
        cell.dependencies.forEach{dep -> dep.dependents =
            try {
                reorderForUpdate(dep.dependents.toSet())
            }catch(e: IllegalArgumentException){
                cell.dependencies.remove(dep)
                dep.dependents.remove(cell)
                throw e
            }
        }
    }

    private fun reorderForUpdate(cellsNeedsUpdate: Set<MetaFunc>):MutableList<MetaFunc> {
        val visited = mutableSetOf<MetaCell>()
        val visiting = mutableSetOf<MetaCell>()
        val result = mutableListOf<MetaFunc>()

        fun dfs(cell: MetaCell) {
            if (cell in visiting)
                throw IllegalArgumentException("Cycle detected at $cell")
            if (cell in visited) return

            visiting.add(cell)

            for (dep in cell.dependents) {
                dfs(dep)
            }

            visiting.remove(cell)
            visited.add(cell)
            if (cell in cellsNeedsUpdate)
                result.add(cell as MetaFunc)
        }
        cellsNeedsUpdate.forEach{fc -> dfs(fc)}
        result.reverse()
        return result
    }

    private fun updateDependents(funcCell: MetaFunc) {
        funcCell.dependencies.forEach { dep ->
            if (funcCell !in dep.dependents) {
                dep.dependents.add(funcCell)
            }
        }
    }
}
