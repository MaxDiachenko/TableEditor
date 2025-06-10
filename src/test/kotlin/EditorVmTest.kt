import app.*
import app.data.IDataModel
import app.data.IDataModelReader
import app.data.IDataModelWriter
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EditorVmTest {

    private lateinit var reader: IDataModelReader
    private lateinit var writer: IDataModelWriter
    private lateinit var docVm: IDocumentVm
    private lateinit var dataModel: IDataModel

    private lateinit var docFactory: (IDataModel, Boolean) -> IDocumentVm
    private lateinit var sparseDataModelFactory: (Int, Int) -> IDataModel

    private lateinit var editorVm: EditorVm

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        reader = mockk()
        writer = mockk()
        docVm = mockk(relaxed = true)
        dataModel = mockk()

        docFactory = mockk()
        sparseDataModelFactory = mockk()

        every { sparseDataModelFactory.invoke(1000, 1000) } returns dataModel
        every { docFactory.invoke(dataModel, true) } returns docVm

        editorVm = EditorVm(
            reader,
            writer,
            docFactory,
            sparseDataModelFactory
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `new should create new document`() {
        assertEquals(docVm, editorVm.currentDocument)

        val docVm2 = mockk<IDocumentVm>(relaxed = true)
        every { docFactory.invoke(dataModel, true) } returns docVm2
        editorVm.new()
        assertEquals(docVm2, editorVm.currentDocument)
    }

    @Test
    fun `setUiDisabled should update state flow`() = runTest {
        editorVm.setUiDisabled(true)
        assertEquals(true, editorVm.uiDisabled.first())

        editorVm.setUiDisabled(false)
        assertEquals(false, editorVm.uiDisabled.first())
    }

    @Test
    fun `load should read file and update currentDocument`() {
        val file = File("testfile.txt")
        val readDataModel = mockk<IDataModel>()
        val loadedDocVm = mockk<IDocumentVm>()

        every { reader.read(file, sparseDataModelFactory) } returns readDataModel
        every { docFactory.invoke(readDataModel, true) } returns loadedDocVm

        editorVm.load(file)

        assertEquals(file, editorVm.currentFile)
        assertEquals(loadedDocVm, editorVm.currentDocument)
    }

    @Test
    fun `save should write current document to currentFile`() {
        val file = File("file.txt")
        val documentData = mockk<IDataModel>()

        every { reader.read(file, sparseDataModelFactory) } returns dataModel
        every { docVm.data } returns documentData
        every { writer.write(documentData, file) } just Runs

        every { docFactory.invoke(dataModel, true) } returns docVm

        editorVm.load(file)
        editorVm.save()

        verify { writer.write(documentData, file) }
    }

    @Test
    fun `save should throw if currentFile is null`() {
        assertFailsWith<IllegalStateException> {
            editorVm.save()
        }
    }

    @Test
    fun `saveAs should update currentFile and write`() {
        val file = File("save_as_file.txt")
        val documentData = mockk<IDataModel>()

        every { docVm.data } returns documentData
        every { writer.write(documentData, file) } just Runs

        editorVm.new()

        every { docFactory.invoke(dataModel, true) } returns docVm

        editorVm.saveAs(file)

        assertEquals(file, editorVm.currentFile)
        verify { writer.write(documentData, file) }
    }
}