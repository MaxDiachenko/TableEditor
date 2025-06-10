package app
import app.data.IDataModel
import app.data.IDataModelReader
import app.data.IDataModelWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.*
import java.io.File

class EditorVm(
    private val reader: IDataModelReader,
    private val writer: IDataModelWriter,
    private val docFactory: (IDataModel, Boolean) -> IDocumentVm,
    private val sparseDataModelFactory: (Int, Int) -> IDataModel
): IEditorVm, KoinComponent {
    override var currentFile: File? = null
        private set
    override var currentDocument: IDocumentVm = createNew()
        private set

    private val _uiDisabled = MutableStateFlow(false)
    override val uiDisabled: StateFlow<Boolean> = _uiDisabled

    override fun setUiDisabled(disabled: Boolean) {
        _uiDisabled.value = disabled
    }

    private fun createNew(): IDocumentVm = docFactory(sparseDataModelFactory(1000, 1000), true)

    override fun load(file: File) {
        currentFile = file
        val data = reader.read(file, sparseDataModelFactory)
        currentDocument = docFactory(data, true)
    }

    override fun save() {
        if(currentFile == null) throw IllegalStateException("Editor wrong state")
        writer.write(currentDocument.data, currentFile!!)
    }

    override fun saveAs(file: File) {
        currentFile = file
        writer.write(currentDocument.data, file)
    }

    override fun new() {
        currentDocument = createNew()
    }
}