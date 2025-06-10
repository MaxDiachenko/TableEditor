import app.expressions.*
import app.expressions.values.*
import kotlin.test.*

class EvaluatorBaseTest {

    private lateinit var evaluator: TestEvaluator

    @BeforeTest
    fun setup() {
        evaluator = TestEvaluator()
    }

    @Test
    fun `eval literal value returns same`() {
        assertEquals(IntVal(5), evaluator.evalPublic(IntVal(5)))
    }

    @Test
    fun `eval unary plus returns same value`() {
        val expr = UnaryExpression(OperatorType.PLUS, IntVal(7))
        assertEquals(IntVal(7), evaluator.evalPublic(expr))
    }

    @Test
    fun `eval unary minus negates value`() {
        val expr = UnaryExpression(OperatorType.MINUS, IntVal(4))
        assertEquals(IntVal(-4), evaluator.evalPublic(expr))
    }

    @Test
    fun `eval unary NOT inverts boolean`() {
        val expr = UnaryExpression(OperatorType.NOT, BoolVal(false))
        assertEquals(BoolVal(true), evaluator.evalPublic(expr))
    }

    @Test
    fun `eval binary expression addition`() {
        val expr = BinaryExpression(IntVal(2), OperatorType.PLUS, IntVal(3))
        assertEquals(IntVal(5), evaluator.evalPublic(expr))
    }

    @Test
    fun `eval binary expression division`() {
        val expr = BinaryExpression(IntVal(10), OperatorType.DIVIDE, IntVal(2))
        assertEquals(DoubleVal(5.0), evaluator.evalPublic(expr))
    }

    @Test
    fun `eval binary logical AND`() {
        val expr = BinaryExpression(BoolVal(true), OperatorType.AND, BoolVal(false))
        assertEquals(BoolVal(false), evaluator.evalPublic(expr))
    }

    @Test
    fun `eval function SUM`() {
        val expr = FunctionCallExpression("SUM", listOf(IntVal(1), IntVal(2), IntVal(3)))
        assertEquals(IntVal(6), evaluator.evalPublic(expr))
    }

    @Test
    fun `eval function AVG`() {
        val expr = FunctionCallExpression("AVG", listOf(IntVal(3), IntVal(3), IntVal(3)))
        assertEquals(DoubleVal(3.0), evaluator.evalPublic(expr))
    }

    @Test
    fun `eval function ABS`() {
        val expr = FunctionCallExpression("ABS", listOf(IntVal(-5)))
        assertEquals(DoubleVal(5.0), evaluator.evalPublic(expr))
    }

    @Test
    fun `eval cell reference`() {
        val result = evaluator.evalPublic(CellRef(0, 0))
        assertEquals(IntVal(10), result)
    }

    @Test
    fun `eval unknown expression throws`() {
        val unknownExpr = object : Expression {}
        val ex = assertFailsWith<IllegalStateException> {
            evaluator.evalPublic(unknownExpr)
        }
        assertTrue(ex.message!!.contains("Unknown expression type"))
    }

    @Test
    fun `eval unsupported unary throws`() {
        val expr = UnaryExpression(OperatorType.AND, IntVal(1))
        val ex = assertFailsWith<IllegalStateException> {
            evaluator.evalPublic(expr)
        }
        assertTrue(ex.message!!.contains("Unsupported unary operation"))
    }

    @Test
    fun `eval unsupported binary throws`() {
        val expr = BinaryExpression(IntVal(1), OperatorType.NOT, IntVal(1))
        val ex = assertFailsWith<IllegalStateException> {
            evaluator.evalPublic(expr)
        }
        assertTrue(ex.message!!.contains("Unsupported binary operation"))
    }

    @Test
    fun `eval unsupported function throws`() {
        val expr = FunctionCallExpression("FOO", listOf(IntVal(1)))
        val ex = assertFailsWith<IllegalStateException> {
            evaluator.evalPublic(expr)
        }
        assertTrue(ex.message!!.contains("Unsupported function"))
    }
}

class TestEvaluator : EvaluatorBase() {
    override fun evalCellRef(expr: CellRef): CellValue? {
        return when (expr) {
            CellRef(0, 0) -> IntVal(10)
            CellRef(1, 1) -> BoolVal(true)
            else -> null
        }
    }

    fun evalPublic(expr: Expression): CellValue? = eval(expr)
}
