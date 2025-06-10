import app.expressions.*
import app.expressions.values.*
import kotlin.test.*

class LexerTest {

    private fun tokenize(input: String): List<Token> =
        Lexer(input).tokenize().filterNot { it === EOFToken }

    @Test
    fun `tokenize integer literal`() {
        val tokens = tokenize("123")
        assertEquals(listOf(IntVal(123)), tokens)
    }

    @Test
    fun `tokenize decimal number`() {
        val tokens = tokenize("3.14")
        assertEquals(listOf(DoubleVal(3.14)), tokens)
    }

    @Test
    fun `tokenize boolean literals`() {
        assertEquals(listOf(BoolVal(true)), tokenize("true"))
        assertEquals(listOf(BoolVal(false)), tokenize("FALSE"))
    }

    @Test
    fun `tokenize simple arithmetic expression`() {
        val tokens = tokenize("1 + 2")
        assertEquals(
            listOf(IntVal(1), OperatorToken(OperatorType.PLUS), IntVal(2)),
            tokens
        )
    }

    @Test
    fun `tokenize parenthesis and comma`() {
        val tokens = tokenize("(1,2)")
        assertEquals(
            listOf(LParenToken, IntVal(1), CommaToken, IntVal(2), RParenToken),
            tokens
        )
    }

    @Test
    fun `tokenize functions and cell refs`() {
        val tokens = tokenize("SUM(A1, B2)")
        assertEquals(
            listOf(
                FunctionToken("SUM"),
                LParenToken,
                CellRefToken("A1"),
                CommaToken,
                CellRefToken("B2"),
                RParenToken
            ),
            tokens
        )
    }

    @Test
    fun `tokenize colon as range`() {
        val tokens = tokenize("A1:B2")
        assertEquals(
            listOf(
                CellRefToken("A1"),
                ColonToken,
                CellRefToken("B2")
            ),
            tokens
        )
    }

    @Test
    fun `tokenize logical operators`() {
        val tokens = tokenize("NOT A1 AND B2 OR TRUE")
        assertEquals(
            listOf(
                OperatorToken(OperatorType.NOT),
                CellRefToken("A1"),
                OperatorToken(OperatorType.AND),
                CellRefToken("B2"),
                OperatorToken(OperatorType.OR),
                BoolVal(true)
            ),
            tokens
        )
    }

    @Test
    fun `invalid character throws exception`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            tokenize("A1 $ B2")
        }
        assertTrue(ex.message!!.contains("Invalid char"))
    }
}
