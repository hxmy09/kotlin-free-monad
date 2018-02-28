package bob.free

import com.github.kittinunf.result.Result
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.*
import org.junit.Test

class FunctionsTest {
    @Test
    fun `test function interpreter`() {
        val exp: Expression<String> = mock()
        assertNull(FunctionInterpreter.eval(exp, mock()))

        val fExpression = FunctionExpression<String>(mock())
        val i: Interpreter = mock()
        val f = FunctionInterpreter.eval(fExpression, i)
        assertNotNull(f)

        f?.invoke()
        verify(fExpression.f).invoke(i)
    }

    @Test
    fun `test expr extension`() {
        val f: Interpreter.() -> String = mock()
        val exp = expr(f)
        assertEquals(f, exp.f)
    }

    @Test
    fun `test map`() {
        val a: Expression<String> = mock()
        val b: (String) -> String = mock()
        val mapped = a.map(b)
        assertTrue(mapped is FunctionExpression<String>)
        (mapped as FunctionExpression<String>)
                .let {
                    val partlyInterpreter: PartlyInterpreter = mock()
                    val interpreter = ComposedInterpreter(listOf(FunctionInterpreter, partlyInterpreter))
                    whenever(partlyInterpreter.eval(a, interpreter)).thenReturn({ "a" })
                    whenever(b.invoke(any())).thenReturn("b")
                    assertEquals("b", interpreter.eval(mapped).get())

                    clearInvocations(b)
                    val exception = ExpressionError.InvokeError(a, RuntimeException())
                    whenever(partlyInterpreter.eval(a, interpreter)).thenThrow(exception)
                    assertEquals(Result.error(exception), interpreter.eval(mapped))
                    verifyZeroInteractions(b)
                }
    }

    @Test
    fun `test flatMap`() {
        val a: Expression<String> = mock()
        val b: (String) -> Expression<String> = mock()
        val mapped = a.flatMap(b)
        assertTrue(mapped is FunctionExpression<String>)
        (mapped as FunctionExpression<String>)
                .let {
                    val partlyInterpreter: PartlyInterpreter = mock()
                    val interpreter = ComposedInterpreter(listOf(FunctionInterpreter, partlyInterpreter))
                    val bExp: Expression<String> = mock()
                    whenever(partlyInterpreter.eval(a, interpreter)).thenReturn { "a" }
                    whenever(partlyInterpreter.eval(bExp, interpreter)).thenReturn { "b" }
                    whenever(b.invoke(any())).thenReturn(bExp)
                    assertEquals("b", interpreter.eval(mapped).get())

                    clearInvocations(b, bExp)
                    val exception = ExpressionError.InvokeError(a, RuntimeException())
                    whenever(partlyInterpreter.eval(a, interpreter)).thenThrow(exception)
                    assertEquals(Result.error(exception), interpreter.eval(mapped))
                    verifyZeroInteractions(b, bExp)
                }
    }

    @Test
    fun `test mapResult`() {
        val a: Expression<String> = mock()
        val b: (Result<String, Exception>) -> String = mock()
        val mapped = a.mapResult(b)
        assertTrue(mapped is FunctionExpression<String>)
        (mapped as FunctionExpression<String>)
                .let {
                    val partlyInterpreter: PartlyInterpreter = mock()
                    val interpreter = ComposedInterpreter(listOf(FunctionInterpreter, partlyInterpreter))
                    whenever(partlyInterpreter.eval(a, interpreter)).thenReturn({ "a" })
                    whenever(b.invoke(Result.of("a"))).thenReturn("b")
                    assertEquals("b", interpreter.eval(mapped).get())

                    val exception = ExpressionError.InvokeError(a, RuntimeException())
                    whenever(partlyInterpreter.eval(a, interpreter)).thenThrow(exception)
                    whenever(b.invoke(Result.error(exception))).thenReturn("exception")
                    assertEquals("exception", interpreter.eval(mapped).get())

                    val invokeException = RuntimeException()
                    whenever(b.invoke(any())).thenThrow(invokeException)
                    assertEquals(invokeException, (interpreter.eval(mapped).component2() as ExpressionError.InvokeError).exception)
                }
    }

    @Test
    fun `test flatMapResult`() {
        val a: Expression<String> = mock()
        val b: (Result<String, Exception>) -> Expression<String> = mock()
        val mapped = a.flatMapResult(b)
        assertTrue(mapped is FunctionExpression<String>)
        (mapped as FunctionExpression<String>)
                .let {
                    val partlyInterpreter: PartlyInterpreter = mock()
                    val interpreter = ComposedInterpreter(listOf(FunctionInterpreter, partlyInterpreter))
                    val bExp: Expression<String> = mock()
                    whenever(partlyInterpreter.eval(a, interpreter)).thenReturn { "a" }
                    whenever(partlyInterpreter.eval(bExp, interpreter)).thenReturn { "b" }
                    whenever(b.invoke(any())).thenReturn(bExp)
                    assertEquals("b", interpreter.eval(mapped).get())

                    val exception = ExpressionError.InvokeError(a, RuntimeException())
                    whenever(partlyInterpreter.eval(a, interpreter)).thenThrow(exception)
                    assertEquals("b", interpreter.eval(mapped).get())

                    val e = RuntimeException()
                    whenever(b.invoke(any())).thenThrow(ExpressionError.EvalError(bExp, e))
                    assertEquals(Result.error(ExpressionError.EvalError(bExp, e)), interpreter.eval(mapped))
                }
    }
}