package ui

import app.*
import app.data.CellType
import javax.swing.table.TableCellEditor
import ui.cellEdits.DatePickerCellEditorComponent
import ui.cellEdits.DefaultCellEditorComponent
import ui.cellEdits.NumericCellEditorComponent
import java.awt.Component
import javax.swing.*
import kotlin.system.exitProcess

class AutoTypedCellEditor(private val document: IDocumentVm) : AbstractCellEditor(), TableCellEditor {
    private var errorPopup: JPopupMenu? = null
    private var currentRow: Int = -1
    private var currentCol: Int = -1
    private lateinit var currentComponent: JComponent

    override fun getTableCellEditorComponent(
        table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int
    ): Component {
        currentRow = row
        currentCol = column

        val result = runCatching {
            val editValue = document.getEditValue(row, column)
            val type = document.getType(row, column)
            Pair(editValue, type)
        }.getOrElse { e ->
            JOptionPane.showMessageDialog(null, e.message, "Internal error", JOptionPane.INFORMATION_MESSAGE)
            exitProcess(-1)
        }
        val editValue = result.first
        val type = result.second

        currentComponent = when (type) {
            CellType.INT, CellType.DOUBLE -> NumericCellEditorComponent(editValue, type)
            CellType.DATETIME -> DatePickerCellEditorComponent(editValue)
            CellType.STRING, CellType.FUNC, CellType.BOOL, null -> DefaultCellEditorComponent(editValue)
        }

        val error = document.getCellError(row, column)
        if(error != null) {
            showErrorPopup(table, error, currentComponent, row, column)
        }

        return currentComponent
    }

    override fun getCellEditorValue(): Any {
        val type = runCatching {
            document.getType(currentRow, currentCol)
        }.getOrElse { e ->
            JOptionPane.showMessageDialog(null, e.message, "Internal error", JOptionPane.INFORMATION_MESSAGE)
            exitProcess(-1)
        }
        return when (type) {
            CellType.INT, CellType.DOUBLE -> (currentComponent as NumericCellEditorComponent).getValue()
            CellType.DATETIME -> (currentComponent as DatePickerCellEditorComponent).getValue()
            CellType.STRING, CellType.FUNC, CellType.BOOL, null -> (currentComponent as DefaultCellEditorComponent).getValue()
        }
    }

    override fun stopCellEditing(): Boolean {
        errorPopup?.isVisible = false
        return super.stopCellEditing()
    }

    private fun showErrorPopup(table: JTable, error: String, editorComponent: Component, row: Int, column: Int,) {
        errorPopup?.isVisible = false
        val popup = JPopupMenu()
        val label = JLabel("<html><body style='color:#aa5555;'>$error</body></html>")
        popup.add(label)
        popup.isFocusable = false

        SwingUtilities.invokeLater {
            if (editorComponent.isShowing) {
                popup.show(editorComponent, editorComponent.width+1, 0)
                errorPopup = popup
            }
        }
    }
}
