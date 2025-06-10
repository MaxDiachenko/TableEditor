import app.data.*
import app.expressions.*
import io.mockk.*
import kotlin.test.*

class DependencyBuilderTest {

    private lateinit var model: IDataModel

    @BeforeTest
    fun setup() {
        model = mockk()
    }

    @Test
    fun `buildAllDependencies returns correct update order`() {

        val c = MetaFunc(CellRef(0, 2))
        val b = MetaFunc(CellRef(0, 1)).apply { dependencies.add(c) }
        val d = MetaFunc(CellRef(0, 0)).apply { dependencies.add(b) }

        val a = MetaCell(mutableListOf(b, c))
        c.dependents.add(b)
        b.dependents.add(d)

        every { model.rowCount } returns 1
        every { model.columnCount } returns 4
        every { model.getMeta(CellRef(0,0)) } returns a
        every { model.getMeta(CellRef(0,1)) } returns b
        every { model.getMeta(CellRef(0,2)) } returns c
        every { model.getMeta(CellRef(0,3)) } returns d
        every { model.formulas } returns setOf(b, c, d)

        val result = DependencyBuilder.buildAllDependencies(model)

        assertEquals(listOf(c), result)
        assertEquals(listOf(c, b), a.dependents)
    }

    @Test
    fun `buildAllDependencies returns correct root`() {
        val c = MetaFunc(CellRef(0, 2))
        val b = MetaFunc(CellRef(0, 1)).apply { dependencies.add(c) }
        val a = MetaFunc(CellRef(0, 0)).apply { dependencies.add(b) }

        c.dependents.add(b)
        b.dependents.add(a)

        every { model.rowCount } returns 1
        every { model.columnCount } returns 3
        every { model.getMeta(CellRef(0,0)) } returns a
        every { model.getMeta(CellRef(0,1)) } returns b
        every { model.getMeta(CellRef(0,2)) } returns c
        every { model.formulas } returns setOf(a, b, c)

        val result = DependencyBuilder.buildAllDependencies(model)

        assertEquals(listOf(c), result)
    }

    @Test
    fun `MetaCell dependency does not affect roots`() {
        val meta = object : MetaCell(mutableListOf()) {}
        val depFunc = MetaFunc(CellRef(0, 1)).apply { dependencies.add(meta) }
        val func = MetaFunc(CellRef(0, 0)).apply { dependencies.add(meta); dependencies.add(depFunc) }
        meta.dependents.addAll(listOf( func, depFunc))
        depFunc.dependents.add(func)
        every { model.rowCount } returns 1
        every { model.columnCount } returns 2
        every { model.getMeta(CellRef(0,0)) } returns func
        every { model.getMeta(CellRef(0,1)) } returns depFunc
        every { model.formulas } returns setOf(func, depFunc)

        val result = DependencyBuilder.buildAllDependencies(model)
        assertEquals(listOf(depFunc), result)
    }

    @Test
    fun `MetaFunc dependency does not affect multiple roots`() {
        val meta = object : MetaCell(mutableListOf()) {}
        val func = MetaFunc(CellRef(0, 0)).apply { dependencies.add(meta)}
        val func2 = MetaFunc(CellRef(0, 1)).apply { dependencies.add(meta) }

        meta.dependents.addAll(listOf( func, func2))
        every { model.rowCount } returns 1
        every { model.columnCount } returns 2
        every { model.getMeta(CellRef(0,0)) } returns func
        every { model.getMeta(CellRef(0,1)) } returns func2
        every { model.formulas } returns setOf(func, func2)

        val result = DependencyBuilder.buildAllDependencies(model)
        assertEquals(listOf(func2, func), result)
    }

    @Test
    fun `cycle detection throws IllegalArgumentException`() {
        val x = MetaFunc(CellRef(0, 0))
        x.dependencies.add(x)
        x.dependents.add(x)

        every { model.rowCount } returns 1
        every { model.columnCount } returns 1
        every { model.getMeta(match { it.row == 0 && it.col == 0 }) } returns x
        every { model.formulas } returns setOf(x)

        val ex = assertFailsWith<IllegalArgumentException> {
            DependencyBuilder.buildAllDependencies(model)
        }
        assertTrue(ex.message!!.contains("Cycle detected"))
    }

    @Test
    fun `updateCellDependencies populates dependents correctly`() {
        val a = MetaFunc(CellRef(0, 0))
        val b = MetaFunc(CellRef(0, 1))
        val c = MetaFunc(CellRef(0, 2))

        a.dependencies.add(b)
        b.dependencies.add(c)

        DependencyBuilder.updateCellDependencies(a)
        DependencyBuilder.updateCellDependencies(b)

        assertEquals(listOf(a), b.dependents)
        assertEquals(listOf(b), c.dependents)
    }
}
