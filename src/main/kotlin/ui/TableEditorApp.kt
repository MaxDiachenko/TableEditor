package ui
import app.IEditorVm
import org.koin.core.component.*
import javax.swing.*

class TableEditorApp : KoinComponent {
    private val editor: IEditorVm by inject()

    fun start() {
        SwingUtilities.invokeLater {
            val mainWindow = MainWindow(editor)
            mainWindow.isVisible = true
        }
    }
}