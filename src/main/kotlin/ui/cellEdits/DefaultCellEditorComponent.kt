package ui.cellEdits
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*

class DefaultCellEditorComponent(value: Any?) : JTextField() {

    init {
        text = value?.toString() ?: ""
        horizontalAlignment = JTextField.LEFT
        addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                SwingUtilities.getAncestorOfClass(JTable::class.java, this@DefaultCellEditorComponent)?.let { table ->
                    (table as JTable).cellEditor?.stopCellEditing()
                }
            }
        })
    }

    fun getValue(): Any {
        return text.trim()
    }
}
