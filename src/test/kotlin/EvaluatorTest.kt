import app.data.IDataModel
import app.data.MetaFunc
import app.expressions.*
import app.expressions.values.*
import io.mockk.*
import kotlin.test.*

class EvaluatorTest {

    private lateinit var data: IDataModel
    private lateinit var evaluator: Evaluator

    @BeforeTest
    fun setUp() {
        data = mockk()
        evaluator = Evaluator(data)
    }

    @Test
    fun `evaluate returns direct value from cell`() {
        val ref = CellRef(1, 2)
        every { data.getMeta(ref) } returns null
        every { data.getValue(ref) } returns IntVal(42)

        val result = evaluator.evaluate(ref)
        assertEquals(IntVal(42), result)
    }

    @Test
    fun `evaluate returns MetaFunc result if present`() {
        val ref = CellRef(2, 3)
        val meta = MetaFunc(ref)
        meta.result = IntVal(99)

        every { data.getMeta(ref) } returns meta

        val result = evaluator.evaluate(ref)
        assertEquals(IntVal(99), result)
    }

    @Test
    fun `evaluate throws if MetaFunc has null result`() {
        val ref = CellRef(3, 4)
        val meta = MetaFunc(ref)

        every { data.getMeta(ref) } returns meta

        val ex = assertFailsWith<IllegalArgumentException> {
            evaluator.evaluate(ref)
        }
        assertEquals("Dependency has an error.", ex.message)
    }

    @Test
    fun `evaluate trows for unknown expressions`() {
        val unknown = object : Expression {}
        val ex = assertFailsWith<IllegalArgumentException> {
            evaluator.evaluate(unknown)
        }

        assertTrue { ex.message?.contains("Unknown expression type") == true }
    }

    @Test
    fun `evaluate catches internal exceptions and rethrows`() {
        val expr = UnaryExpression(OperatorType.MULTIPLY, IntVal(1))

        val ex = assertFailsWith<IllegalArgumentException> {
            evaluator.evaluate(expr)
        }

        assertTrue(ex.message!!.contains("Unsupported unary operation"))
    }
}
