import app.expressions.*
import app.expressions.Parser.parse
import app.expressions.values.*
import io.mockk.*
import kotlin.test.*

class ParserTest {

    @BeforeTest
    fun setup() {
        mockkConstructor(Lexer::class)
        mockkObject(Optimizer)
        every { Optimizer.optimize(any()) } answers { firstArg() }
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `parse unary plus`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(
            OperatorToken(OperatorType.PLUS),
            IntVal(5),
            EOFToken
        )
        assertEquals(UnaryExpression(OperatorType.PLUS, IntVal(5)), parse("=+5"))
    }

    @Test
    fun `parse unary minus`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(
            OperatorToken(OperatorType.MINUS),
            IntVal(7),
            EOFToken
        )
        assertEquals(UnaryExpression(OperatorType.MINUS, IntVal(7)), parse("=-7"))
    }

    @Test
    fun `parse NOT unary expression`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(
            OperatorToken(OperatorType.NOT),
            BoolVal(false),
            EOFToken
        )
        assertEquals(UnaryExpression(OperatorType.NOT, BoolVal(false)), parse("=NOT false"))
    }

    @Test
    fun `parse binary precedence 1 + 2 * 3`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(
            IntVal(1),
            OperatorToken(OperatorType.PLUS),
            IntVal(2),
            OperatorToken(OperatorType.MULTIPLY),
            IntVal(3),
            EOFToken
        )
        val expr = parse("=1 + 2 * 3")
        assertEquals(
            BinaryExpression(
                IntVal(1),
                OperatorType.PLUS,
                BinaryExpression(IntVal(2), OperatorType.MULTIPLY, IntVal(3))
            ),
            expr
        )
    }

    @Test
    fun `parse parenthesized expression`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(
            LParenToken,
            IntVal(1),
            OperatorToken(OperatorType.PLUS),
            IntVal(2),
            RParenToken,
            OperatorToken(OperatorType.MULTIPLY),
            IntVal(3),
            EOFToken
        )
        val expr = parse("=(1 + 2) * 3")
        assertEquals(
            BinaryExpression(
                BinaryExpression(IntVal(1), OperatorType.PLUS, IntVal(2)),
                OperatorType.MULTIPLY,
                IntVal(3)
            ),
            expr
        )
    }

    @Test
    fun `parse function call with range`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(
            FunctionToken("SUM"),
            LParenToken,
            CellRefToken("A1"),
            ColonToken,
            CellRefToken("B2"),
            RParenToken,
            EOFToken
        )
        val result = parse("=SUM(A1:B2)")
        assertTrue(result is FunctionCallExpression)
        assertEquals("SUM", (result as FunctionCallExpression).name)
    }

    @Test
    fun `parse function call with arguments`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(
            FunctionToken("AVG"),
            LParenToken,
            IntVal(10),
            CommaToken,
            IntVal(20),
            RParenToken,
            EOFToken
        )
        assertEquals(
            FunctionCallExpression("AVG", listOf(IntVal(10), IntVal(20))),
            parse("=AVG(10, 20)")
        )
    }

    @Test
    fun `parse empty function call`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(
            FunctionToken("NOW"),
            LParenToken,
            RParenToken,
            EOFToken
        )
        assertEquals(FunctionCallExpression("NOW", listOf()), parse("=NOW()"))
    }

    @Test
    fun `unexpected EOF throws`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(EOFToken)
        assertFailsWith<IllegalArgumentException> { parse("=") }
    }

    @Test
    fun `unexpected token throws`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(
            OperatorToken(OperatorType.AND),
            EOFToken
        )
        val ex = assertFailsWith<IllegalArgumentException> {
            parse("=AND")
        }
        assertTrue(ex.message!!.contains("Unexpected token"))
    }

    @Test
    fun `invalid range syntax throws`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(
            FunctionToken("SUM"),
            LParenToken,
            CellRefToken("A1"),
            ColonToken,
            IntVal(42),
            RParenToken,
            EOFToken
        )
        val ex = assertFailsWith<IllegalArgumentException> {
            parse("=SUM(A1:42)")
        }
        assertTrue(ex.message!!.contains("Unexpected identifier"))
    }

    @Test
    fun `mismatched parentheses throws`() {
        every { anyConstructed<Lexer>().tokenize() } returns listOf(
            LParenToken,
            IntVal(1),
            OperatorToken(OperatorType.PLUS),
            IntVal(2),
            EOFToken
        )
        assertFailsWith<IllegalStateException> {
            parse("=(1 + 2")
        }
    }
}
