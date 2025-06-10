package app

import java.io.File
import kotlinx.coroutines.flow.*

interface IEditorVm {
    val currentDocument: IDocumentVm
    val currentFile: File?
    val uiDisabled: StateFlow<Boolean>
    fun load(file: File): Unit
    fun save(): Unit
    fun new(): Unit
    fun saveAs(file: File)
    fun setUiDisabled(disabled: Boolean)
}
