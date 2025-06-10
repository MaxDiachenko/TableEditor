package ui.cellEdits
import app.data.CellType
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent

import javax.swing.*

class NumericCellEditorComponent(value: Any?, private val cellType: CellType?) : JTextField() {

    init {
        text = value?.toString() ?: ""
        horizontalAlignment = JTextField.RIGHT
        addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                SwingUtilities.getAncestorOfClass(JTable::class.java, this@NumericCellEditorComponent)?.let { table ->
                    (table as JTable).cellEditor?.stopCellEditing()
                }
            }
        })
    }

    fun getValue(): Any {
        val str = text.trim()
        return when (cellType) {
            CellType.INT -> str.toIntOrNull() ?: str
            CellType.DOUBLE -> str.toDoubleOrNull() ?: str
            else -> str
        }
    }

}
