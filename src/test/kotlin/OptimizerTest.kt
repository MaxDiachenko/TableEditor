import app.expressions.*
import app.expressions.Parser.parse
import app.expressions.values.*
import kotlin.test.*

class OptimizerTest {

    @Test
    fun `optimize simple constant binary expression`() {
        val expr = BinaryExpression(IntVal(3), OperatorType.PLUS, IntVal(4))
        val optimized = Optimizer.optimize(expr)
        assertEquals(IntVal(7), optimized)
    }

    @Test
    fun `optimize nested constant binary expressions`() {
        val expr = BinaryExpression(
            BinaryExpression(IntVal(2), OperatorType.MULTIPLY, IntVal(5)),
            OperatorType.MINUS,
            IntVal(4)
        )
        val optimized = Optimizer.optimize(expr)
        assertEquals(IntVal(6), optimized)
    }

    @Test
    fun `optimize unary expression`() {
        val expr = UnaryExpression(OperatorType.MINUS, IntVal(3))
        val optimized = Optimizer.optimize(expr)
        assertEquals(IntVal(-3), optimized)
    }

    @Test
    fun `do not optimize expression with CellRef`() {
        val expr = BinaryExpression(IntVal(2), OperatorType.PLUS, CellRef(0, 0))
        val optimized = Optimizer.optimize(expr)
        assertEquals(expr, optimized)
    }

    @Test
    fun `do not optimize function expression with cell refs`() {
        val expr = FunctionCallExpression("SUM", listOf(CellRef(0, 0), IntVal(5)))
        val optimized = Optimizer.optimize(expr)
        assertEquals(expr, optimized)
    }

    @Test
    fun `optimize function call with constant args`() {
        val expr = FunctionCallExpression("SUM", listOf(IntVal(1), IntVal(2), IntVal(3)))
        val optimized = Optimizer.optimize(expr)
        assertEquals(IntVal(6), optimized)
    }

    @Test
    fun `unsupported function trows`() {
        val expr = FunctionCallExpression("CUSTOM", listOf(IntVal(1)))
        val ex = assertFailsWith<IllegalStateException> {
            Optimizer.optimize(expr)
        }
        assertTrue(ex.message!!.contains("Unsupported function"))
    }

    @Test
    fun `mixed static and dynamic function does not optimize`() {
        val expr = FunctionCallExpression("SUM", listOf(IntVal(10), CellRef(1, 1)))
        val optimized = Optimizer.optimize(expr)
        assertEquals(expr, optimized)
    }

    @Test
    fun `optimize deeply nested constants`() {
        val expr = BinaryExpression(
            BinaryExpression(IntVal(1), OperatorType.PLUS, IntVal(2)),
            OperatorType.MULTIPLY,
            BinaryExpression(IntVal(3), OperatorType.MINUS, IntVal(1))
        )
        val optimized = Optimizer.optimize(expr)
        assertEquals(IntVal(6), optimized)
    }
}
