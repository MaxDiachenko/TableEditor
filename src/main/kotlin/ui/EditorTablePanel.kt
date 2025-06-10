package ui
import app.IEditorVm
import app.data.CellType
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.*
import kotlinx.coroutines.flow.*

class EditorTablePanel(private val editor: IEditorVm) : JPanel(BorderLayout()) {

    val model = JFrameTableModel(editor)
    private val table = object : JTable(model) {
        override fun getDefaultEditor(columnClass: Class<*>?): TableCellEditor {
            return AutoTypedCellEditor(editor.currentDocument)
        }
        override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
            val c = super.prepareRenderer(renderer, row, column)

            val error = editor.currentDocument.getCellError(row, column)
            if (error != null) {
                c.background = if (error == "Dependency has an error.") Color(255, 255, 204) else Color(255, 200, 200)
                if (c is JComponent) {
                    c.toolTipText = error
                }
            } else {
                c.background = if (isCellSelected(row, column)) selectionBackground else background
                if (c is JComponent) {
                    c.toolTipText = null
                }

                val type = editor.currentDocument.getPresentationType(row, column)

                if (c is JLabel) {
                    c.horizontalAlignment = when (type) {
                        CellType.INT, CellType.DOUBLE -> SwingConstants.RIGHT
                        CellType.STRING, CellType.FUNC -> SwingConstants.LEFT
                        CellType.DATETIME -> SwingConstants.CENTER
                        else -> SwingConstants.LEFT
                    }
                }
            }


            return c
        }
    }

    init {
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.putClientProperty("JTable.autoStartsEdit", false)
        table.cellSelectionEnabled = true
        table.rowSelectionAllowed = true
        table.rowHeight = 20
        repeat(model.columnCount) {
            table.columnModel.getColumn(it).preferredWidth = 75
        }
        table.tableHeader.reorderingAllowed = false

        val rowHeader = JList((1..model.rowCount).toList().toTypedArray()).apply {
            fixedCellWidth = 40
            fixedCellHeight = table.rowHeight
            cellRenderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                    val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                    label.horizontalAlignment = SwingConstants.CENTER
                    return label
                }
            }
        }

        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (table.isEditing && !SwingUtilities.isDescendingFrom(e.component, table)) {
                    table.cellEditor?.stopCellEditing()
                }
            }
        })

        val scrollPane = JScrollPane(table).apply {
            setRowHeaderView(rowHeader)
        }

        addColumnHeaderContextMenu(table)
        addRowHeaderContextMenu(rowHeader)

        add(scrollPane, BorderLayout.CENTER)

        CoroutineScope(Dispatchers.Main.immediate).launch {
            editor.uiDisabled.collect { disabled ->
                table.isEnabled = !disabled
            }
        }
    }

    fun refresh() {
        model.fireTableStructureChanged()
    }

    private fun addRowHeaderContextMenu(rowHeader: JList<*>) {
        val popup = JPopupMenu()

        val insertBefore = JMenuItem("Insert Row Before")
        val insertAfter = JMenuItem("Insert Row After")
        val remove = JMenuItem("Remove Row")

        var clickedRow = -1

        insertBefore.addActionListener {
            if (clickedRow >= 0) {
                performWithUiLock {
                    editor.currentDocument.addRowBefore(clickedRow)
                }
            }
        }

        insertAfter.addActionListener {
            if (clickedRow >= 0) {
                performWithUiLock {
                    editor.currentDocument.addRowAfter(clickedRow)
                }
            }
        }

        remove.addActionListener {
            if (clickedRow >= 0) {
                performWithUiLock {
                    editor.currentDocument.removeRow(clickedRow)
                }
            }
        }

        popup.add(insertBefore)
        popup.add(insertAfter)
        popup.add(remove)

        rowHeader.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) showPopup(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) showPopup(e)
            }

            private fun showPopup(e: MouseEvent) {
                clickedRow = rowHeader.locationToIndex(e.point)
                popup.show(e.component, e.x, e.y)
            }
        })
    }

    private fun addColumnHeaderContextMenu(table: JTable) {
        val header = table.tableHeader
        val popup = JPopupMenu()

        var clickedColumn = -1

        val insertBefore = JMenuItem("Insert Column Before")
        val insertAfter = JMenuItem("Insert Column After")
        val remove = JMenuItem("Remove Column")
        val sortAscending = JMenuItem("Sort Ascending")
        val sortDescending = JMenuItem("Sort Descending")

        insertBefore.addActionListener {
            if (clickedColumn  >= 0) {
                performWithUiLock {
                    editor.currentDocument.addColumnBefore(clickedColumn)
                }
            }
        }

        insertAfter.addActionListener {
            if (clickedColumn  >= 0) {
                performWithUiLock {
                    editor.currentDocument.addColumnAfter(clickedColumn)
                }
            }
        }

        remove.addActionListener {
            if (clickedColumn  >= 0) {
                performWithUiLock {
                    editor.currentDocument.removeColumn(clickedColumn)
                }
            }
        }

        sortAscending.addActionListener {
            if (clickedColumn  >= 0) {
                performWithUiLock {
                    editor.currentDocument.sortAscending(clickedColumn)
                }
            }
        }

        sortDescending.addActionListener {
            if (clickedColumn  >= 0) {
                performWithUiLock {
                    editor.currentDocument.sortDescending(clickedColumn)
                }
            }
        }

        popup.add(insertBefore)
        popup.add(insertAfter)
        popup.add(remove)
        popup.add(sortAscending)
        popup.add(sortDescending)

        header.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) showPopup(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) showPopup(e)
            }

            private fun showPopup(e: MouseEvent) {
                clickedColumn = header.columnAtPoint(e.point)
                popup.show(e.component, e.x, e.y)
            }
        })
    }

    private fun performWithUiLock(block: suspend () -> Unit) {
        CoroutineScope(Dispatchers.Swing).launch {
            editor.setUiDisabled(true)
            try {
                withContext(Dispatchers.Default) {
                    block()
                }
                refresh()
            } finally {
                editor.setUiDisabled(false)
            }
        }
    }
}
