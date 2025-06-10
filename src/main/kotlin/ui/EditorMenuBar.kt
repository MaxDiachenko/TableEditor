package ui
import app.IEditorVm
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.*
import kotlinx.coroutines.flow.*

class EditorMenuBar(
    private val parentFrame: JFrame,
    private val editor: IEditorVm,
    private val tablePanel: EditorTablePanel
) : JMenuBar() {

    private val newItem = JMenuItem("New").apply {
        addActionListener {
            if (checkUnsavedChanges()) {
                performWithUiLock { editor.new() }
            }
        }
    }

    private val openItem = JMenuItem("Open").apply {
        addActionListener {
            if (checkUnsavedChanges()) {
                val fileChooser = JFileChooser()
                fileChooser.fileFilter = FileNameExtensionFilter("Table files (*.ted)", "ted")
                val result = fileChooser.showOpenDialog(parentFrame)
                if (result == JFileChooser.APPROVE_OPTION) {
                    performWithUiLock {
                        editor.load(fileChooser.selectedFile)
                    }
                }
            }
        }
    }

    private val saveItem = JMenuItem("Save").apply {
        addActionListener {
            if (editor.currentFile != null) {
                performWithUiLock {
                    editor.save()
                }

                return@addActionListener
            }
            val chooser = JFileChooser()
            chooser.fileFilter = FileNameExtensionFilter("Table files (*.ted)", "ted")
            val result = chooser.showSaveDialog(parentFrame)
            if (result == JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                if (!file.name.endsWith(".ted")) {
                    file = File(file.absolutePath + ".ted")
                }
                performWithUiLock {
                    editor.saveAs(file)
                }
            }
        }
    }

    private val saveAsItem = JMenuItem("SaveAs").apply {
        addActionListener {
            val chooser = JFileChooser()
            chooser.fileFilter = FileNameExtensionFilter("Table files (*.ted)", "ted")
            val result = chooser.showSaveDialog(parentFrame)
            if (result == JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                if (!file.name.endsWith(".ted")) {
                    file = File(file.absolutePath + ".ted")
                }
                performWithUiLock {
                    editor.saveAs(file)
                }
            }
        }
    }

    init {
        val fileMenu = JMenu("File")
        fileMenu.add(newItem)
        fileMenu.add(openItem)
        fileMenu.add(saveItem)
        fileMenu.add(saveAsItem)

        add(fileMenu)

        CoroutineScope(Dispatchers.Swing).launch {
            editor.uiDisabled.collect { disabled ->
                setMenuEnabled(!disabled)
            }
        }
    }

    private fun setMenuEnabled(enabled: Boolean) {
        newItem.isEnabled = enabled
        openItem.isEnabled = enabled
        saveItem.isEnabled = enabled
    }

    private fun checkUnsavedChanges(): Boolean {
        if (!editor.currentDocument.hasUnsavedChanges()) {
            return true
        }

        val result = JOptionPane.showConfirmDialog(
            parentFrame,
            "You have unsaved changes. Do you want to continue?",
            "Unsaved Changes",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        return result == JOptionPane.YES_OPTION
    }

    private fun performWithUiLock(block: suspend () -> Unit) {
        CoroutineScope(Dispatchers.Swing).launch {
            editor.setUiDisabled(true)
            try {
                withContext(Dispatchers.Default) {
                    block()
                }
                tablePanel.refresh()
            } finally {
                editor.setUiDisabled(false)
            }
        }
    }
}
