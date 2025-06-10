package ui

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.*
import kotlinx.coroutines.flow.*
import app.*
import java.lang.IllegalArgumentException
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import kotlin.system.exitProcess

class JFrameTableModel(private val editor: IEditorVm) : AbstractTableModel() {

    override fun getRowCount(): Int = editor.currentDocument.rowCount

    override fun getColumnCount(): Int = editor.currentDocument.columnCount

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        try {
            return editor.currentDocument.getVisualValue(rowIndex, columnIndex)
        }catch (e: Exception) {
                JOptionPane.showMessageDialog(null, e.message, "Internal error", JOptionPane.INFORMATION_MESSAGE)
            exitProcess(-1)
            }
    }

    override fun setValueAt(newValue: Any?, row: Int, col: Int) {
        CoroutineScope(Dispatchers.Swing).launch {
            editor.setUiDisabled(true)
            try {
                val updatedCells = withContext(Dispatchers.Default) {
                    editor.currentDocument.setValue(row, col, newValue)
                }
                updatedCells.forEach { p -> fireTableCellUpdated(p.row, p.col) }
            }
            catch (_: IllegalArgumentException) {}
            catch (e: Exception) {
                JOptionPane.showMessageDialog(null, e.message, "Internal error", JOptionPane.INFORMATION_MESSAGE)
                exitProcess(-1)
            } finally {
                editor.setUiDisabled(false)
            }
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

    override fun getColumnClass(columnIndex: Int): Class<*> = Any::class.java
}