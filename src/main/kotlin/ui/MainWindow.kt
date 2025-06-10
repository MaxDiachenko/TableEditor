package ui
import app.IEditorVm
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class MainWindow(private val editor: IEditorVm) : JFrame("Table Editor") {

    private val tablePanel = EditorTablePanel(editor)
    private val menuBar = EditorMenuBar(this, editor, tablePanel)

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        jMenuBar = menuBar
        add(tablePanel, BorderLayout.CENTER)
        size = Dimension(800, 600)

    }
}