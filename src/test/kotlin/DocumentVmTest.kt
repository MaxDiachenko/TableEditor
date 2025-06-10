import app.*
import app.data.*
import app.expressions.*
import app.expressions.values.*
import io.mockk.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class DocumentVmTest {

    private lateinit var model: IDataModel
    private lateinit var cellFactory: ICellFactory
    private lateinit var evaluator: IEvaluator
    private lateinit var denseFactory: (Int, Int) -> IDataModel
    private lateinit var sparseFactory: (Int, Int) -> IDataModel
    private lateinit var evaluatorFactory: (IDataModel) -> IEvaluator

    private lateinit var vm: DocumentVm

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkStatic("app.expressions.ExpressionKt")

        model = mockk(relaxed = true)
        cellFactory = mockk()
        evaluator = mockk(relaxed = true)
        denseFactory = mockk()
        sparseFactory = mockk()
        evaluatorFactory = mockk()

        every { model.rowCount } returns 5
        every { model.columnCount } returns 5
        every { model.cellCount } returns 0
        every { evaluatorFactory.invoke(any()) } returns evaluator
        every { denseFactory(any(), any()) } returns model
        every { sparseFactory(any(), any()) } returns model

        mockkObject(DependencyBuilder)
        every { DependencyBuilder.buildAllDependencies(model) } returns emptyList()

        vm = DocumentVm(cellFactory, denseFactory, sparseFactory, evaluatorFactory, model, true)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
    @Test
    fun `hasUnsavedChanges returns true after setValue`() {
        val ref = CellRef(1, 1)
        val meta = MetaCell(mutableListOf())
        val value = StringVal("hello")

        every { cellFactory.fromAny("hello", ref) } returns Pair(value, meta)
        every { model.getMeta(ref) } returns null

        vm.setValue(1, 1, "hello").toList()
        assertTrue(vm.hasUnsavedChanges())
    }

    @Test
    fun `setValue null clears and triggers change`() {
        val ref = CellRef(0, 0)
        every { model.clear(ref) } just Runs
        every { model.getMeta(ref) } returns null

        val result = vm.setValue(0, 0, null).toList()
        assertEquals(listOf(ref), result)
    }

    @Test
    fun `getVisualValue and getEditValue call model`() {
        val ref = CellRef(2, 3)
        every { model.getEvaluatedValue(ref) } returns StringVal("first")
        every { model.getValue(ref) } returns StringVal("second")

        assertEquals(StringVal("first"), vm.getVisualValue(2, 3))
        assertEquals(StringVal("second"), vm.getEditValue(2, 3))
    }

    @Test
    fun `add and remove rows and columns interact with model`() {
        every { model.canRemoveRow(any()) } returns true
        every { model.canRemoveColumn(any()) } returns true

        vm.addRowBefore(1)
        vm.addRowAfter(2)
        vm.addColumnBefore(3)
        vm.addColumnAfter(4)
        vm.removeRow(0)
        vm.removeColumn(1)

        verify { model.insertRowAt(1) }
        verify { model.insertRowAt(3) }
        verify { model.insertColumnAt(3) }
        verify { model.insertColumnAt(5) }
        verify { model.removeRow(0) }
        verify { model.removeColumn(1) }
    }

    @Test
    fun `removeRow throws if not removable`() {
        every { model.canRemoveRow(0) } returns false
        assertFailsWith<IllegalArgumentException> { vm.removeRow(0) }
    }

    @Test
    fun `removeColumn throws if not removable`() {
        every { model.canRemoveColumn(0) } returns false
        assertFailsWith<IllegalArgumentException> { vm.removeColumn(0) }
    }

    @Test
    fun `sortAscending and sortDescending call model`() {
        vm.sortAscending(1)
        vm.sortDescending(2)

        verify { model.sortByColumn(1, false) }
        verify { model.sortByColumn(2, true) }
    }

    @Test
    fun `getPresentationType returns from MetaFunc result or value`() {
        val ref = CellRef(1, 1)
        val func = MetaFunc(ref)
        func.result = DoubleVal(3.14)
        every { model.getMeta(ref) } returns func

        assertEquals(CellType.DOUBLE, vm.getPresentationType(1, 1))

        every { model.getMeta(ref) } returns null
        every { model.getValue(ref) } returns IntVal(2)
        assertEquals(CellType.INT, vm.getPresentationType(1, 1))
    }

    @Test
    fun `getType returns value cellType`() {
        val ref = CellRef(0, 0)
        every { model.getValue(ref) } returns StringVal("type")
        assertEquals(CellType.STRING, vm.getType(0, 0))
    }

    @Test
    fun `getCellError returns error if MetaFunc has one`() {
        val ref = CellRef(0, 0)
        val meta = MetaFunc(ref).apply { error = "fail" }
        every { model.getMeta(ref) } returns meta

        assertEquals("fail", vm.getCellError(0, 0))

        every { model.getMeta(ref) } returns null
        assertNull(vm.getCellError(0, 0))
    }

    @Test
    fun `checkSwitchDataModel switches from sparse to dense when threshold exceeded`() {
        every { model.rowCount } returns 10
        every { model.columnCount } returns 10
        every { model.cellCount } returns 50

        every { model.getValue(any()) } returns StringVal("sparse")
        val newModel = mockk<IDataModel>(relaxed = true) {
            every { rowCount } returns 10
            every { columnCount } returns 10
            every { getValue(any()) } returns StringVal("dense")
        }

        every { denseFactory(any(), any()) } returns newModel
        every { evaluatorFactory(newModel) } returns evaluator
        every { model.copyTo(newModel) } just Runs

        vm.setValue(0, 0, null).toList()

        assertEquals(StringVal("dense"), vm.getEditValue(0, 0))
    }

    @Test
    fun `checkSwitchDataModel switches from dense to sparse when threshold drops`() {

        val denseModel = mockk<IDataModel>(relaxed = true) {
            every { rowCount } returns 10
            every { columnCount } returns 10
            every { cellCount } returns 50
            every { getValue(any()) } returns StringVal("dense")
        }

        val sparseModel = mockk<IDataModel>(relaxed = true) {
            every { rowCount } returns 10
            every { columnCount } returns 10
            every { getValue(any()) } returns StringVal("sparse")
        }

        val vm = DocumentVm(cellFactory, denseFactory, sparseFactory, evaluatorFactory, denseModel, false)

        every { sparseFactory(10, 10) } returns sparseModel
        every { evaluatorFactory(sparseModel) } returns evaluator
        every { denseModel.copyTo(sparseModel) } just Runs
        every { denseModel.cellCount } returns 29

        vm.setValue(0, 0, null).toList()

        assertEquals(StringVal("sparse"), vm.getEditValue(0, 0))
    }

    @Test
    fun `setValue processes formula and yields changes`() {
        val ref = CellRef(0, 0)
        val formula = MetaFunc(ref)
        val value = StringVal("=A1+B2")

        every { cellFactory.fromAny("=A1+B2", ref) } returns Pair(value, formula)
        every { model.setValue(ref, value) } just Runs
        every { model.setMeta(ref, formula) } just Runs
        every { model.getMeta(ref) } returns formula
        every { model.getOrCreateMeta(any()) } returns MetaCell(mutableListOf())

        mockkObject(Parser)
        mockkObject(DependencyBuilder)

        val cel01 = CellRef(0, 1)
        val expr = mockk<Expression>()
        every { expr.traverse() } returns sequenceOf(cel01)
        every { Parser.parse(value.toString()) } returns expr

        every { DependencyBuilder.updateCellDependencies(any()) } just Runs
        every { evaluator.evaluate(any()) } returns IntVal(10)

        val result = vm.setValue(0, 0, "=A1+B2").toList()
        assertTrue(result.contains(ref))
    }
}