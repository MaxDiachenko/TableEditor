import app.data.*
import app.expressions.*
import io.mockk.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class MetaDataModelTest {

    private lateinit var model: MetaDataModel

    @BeforeEach
    fun setUp() {
        mockkStatic("app.expressions.ExpressionKt")
        model = MetaDataModel(10, 10)
    }

    @Test
    fun `getOrCreateMeta returns same instance on repeated calls`() {
        val ref = CellRef(2, 3)
        val m1 = model.getOrCreateMeta(ref)
        val m2 = model.getOrCreateMeta(ref)
        assertSame(m1, m2)
    }

    @Test
    fun `setMeta stores MetaCell and getMeta retrieves it`() {
        val ref = CellRef(1, 1)
        val meta = MetaCell(mutableListOf())
        model.setMeta(ref, meta)
        assertEquals(meta, model.getMeta(ref))
    }

    @Test
    fun `setMeta with MetaFunc replaces old MetaFunc and preserves dependents`() {
        val ref = CellRef(0, 0)
        val oldFunc = MetaFunc(ref)
        val newFunc = MetaFunc(ref)

        val dep = MetaCell(mutableListOf())
        oldFunc.dependencies.add(dep)
        dep.dependents.add(oldFunc)

        val child = MetaFunc(CellRef(1, 1))
        oldFunc.dependents.add(child)

        model.setMeta(ref, oldFunc)
        model.setMeta(ref, newFunc)

        assertEquals(newFunc, model.getMeta(ref))
        assertTrue(newFunc.dependents.contains(child))
        assertTrue(dep.dependents.isEmpty())
    }

    @Test
    fun `setMeta null removes MetaFunc and clears links`() {
        val ref = CellRef(0, 0)
        val func = MetaFunc(ref)
        val dep = MetaCell(mutableListOf())
        func.dependencies.add(dep)
        dep.dependents.add(func)

        model.setMeta(ref, func)
        model.setMeta(ref, null)

        assertNull(model.getMeta(ref))
        assertTrue(dep.dependents.isEmpty())
    }

    @Test
    fun `shiftMetaRowsRight updates rows in meta and formula refs`() {
        val ref = CellRef(2, 2)
        val func = MetaFunc(ref)
        val expr =  mockk<Expression>()
        val ref22 = CellRef(2, 2)
        val ref32 = CellRef(3, 2)
        every { expr.traverse() } returns sequenceOf(ref22, ref32)
        func.expression = expr

        model.setMeta(ref, func)
        model.shiftMetaRowsRight(2)

        assertNull(model.getMeta(CellRef(2, 2)))
        assertNotNull(model.getMeta(CellRef(3, 2)))
        assertEquals(CellRef(3, 2), ref22)
        assertEquals(CellRef(4, 2), ref32)
    }

    @Test
    fun `shiftMetaRowsLeft updates rows in meta and formula refs`() {
        val ref = CellRef(3, 3)
        val func = MetaFunc(ref)
        val expr =  mockk<Expression>()
        val ref33 = CellRef(3, 3)
        every { expr.traverse() } returns sequenceOf(ref33)
        func.expression = expr

        model.setMeta(ref, func)
        model.shiftMetaRowsLeft(2)

        assertNull(model.getMeta(CellRef(3, 3)))
        assertNotNull(model.getMeta(CellRef(2, 3)))
        assertEquals(CellRef(2, 3), ref33)
    }

    @Test
    fun `shiftMetaColumnsRight updates columns in meta and formula refs`() {
        val ref = CellRef(4, 4)
        val func = MetaFunc(ref)
        val expr =  mockk<Expression>()
        val ref44 = CellRef(4, 4)
        every { expr.traverse() } returns sequenceOf(ref44)
        func.expression = expr

        model.setMeta(ref, func)
        model.shiftMetaColumnsRight(4)

        assertNull(model.getMeta(CellRef(4, 4)))
        assertNotNull(model.getMeta(CellRef(4, 5)))
        assertEquals(CellRef(4, 5), ref44)
    }

    @Test
    fun `shiftMetaColumnsLeft updates columns in meta and formula refs`() {
        val ref = CellRef(5, 5)
        val func = MetaFunc(ref)
        val expr =  mockk<Expression>()
        every { expr.traverse() } returns sequenceOf(CellRef(5, 5))
        func.expression = expr

        model.setMeta(ref, func)
        model.shiftMetaColumnsLeft(4)

        assertNull(model.getMeta(CellRef(5, 5)))
        assertNotNull(model.getMeta(CellRef(5, 4)))
    }

    @Test
    fun `canRemoveRow returns false if any MetaCell has dependents`() {
        val ref = CellRef(2, 2)
        val meta = MetaCell(mutableListOf()).apply {
            dependents.add(MetaFunc(CellRef(1, 1)))
        }
        model.setMeta(ref, meta)
        assertFalse(model.canRemoveRow(2))
    }

    @Test
    fun `canRemoveRow returns true if no dependents`() {
        val ref = CellRef(2, 2)
        model.setMeta(ref, MetaCell(mutableListOf()))
        assertTrue(model.canRemoveRow(2))
    }

    @Test
    fun `canRemoveColumn returns false if any MetaCell has dependents`() {
        val ref = CellRef(1, 3)
        val meta = MetaCell(mutableListOf()).apply {
            dependents.add(MetaFunc(CellRef(0, 0)))
        }
        model.setMeta(ref, meta)
        assertFalse(model.canRemoveColumn(3))
    }

    @Test
    fun `canRemoveColumn returns true if no dependents`() {
        val ref = CellRef(1, 3)
        model.setMeta(ref, MetaCell(mutableListOf()))
        assertTrue(model.canRemoveColumn(3))
    }

    @Test
    fun `remapRows updates row keys and formula CellRefs`() {
        val ref = CellRef(2, 2)
        val func = MetaFunc(ref)
        val first =CellRef(2, 2)
        val second =CellRef(3, 2)
        val expr =  mockk<Expression>()
        every { expr.traverse() } returns sequenceOf(first, second)
        func.expression = expr
        model.setMeta(ref, func)

        val map = IntArray(10) { it }
        map[2] = 7
        map[3] = 8

        model.remapRows(map)

        assertNull(model.getMeta(CellRef(2, 2)))
        assertNotNull(model.getMeta(CellRef(7, 2)))
        assertEquals(7, first.row)
        assertEquals(8, second.row)
    }
}
