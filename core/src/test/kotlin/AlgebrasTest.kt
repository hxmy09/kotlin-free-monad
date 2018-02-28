package bob.free

import com.github.kittinunf.result.Result
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.assertEquals
import org.junit.Test

class AlgebrasTest {
    @Test
    fun `test interpreters compose`() {
        val partlyInterpreters = listOf(mock<PartlyInterpreter>(), mock())
        val interpreter = ComposedInterpreter(partlyInterpreters)
        clearInvocations(*partlyInterpreters.toTypedArray())

        val exp: Expression<String> = mock()
        whenever(partlyInterpreters[0].eval(exp, interpreter)).thenReturn({ "eval1" })
        assertEquals("eval1", interpreter.eval(exp).get())
        verifyZeroInteractions(partlyInterpreters[1])

        clearInvocations(partlyInterpreters[0])
        whenever(partlyInterpreters[0].eval(exp, interpreter)).thenReturn(null)
        whenever(partlyInterpreters[1].eval(exp, interpreter)).thenReturn({ "eval2" })
        assertEquals("eval2", interpreter.eval(exp).get())
        verify(partlyInterpreters[0]).eval(exp, interpreter)
    }

    @Test
    fun `test interpreters compose order`() {
        val i = mock<PartlyInterpreter>().also {
            whenever(it.priority).thenReturn(1)
            whenever(it.eval<String>(any(), any())).thenReturn { "1" }
        }
        val i2 = mock<PartlyInterpreter>().also {
            whenever(it.priority).thenReturn(2)
            whenever(it.eval<String>(any(), any())).thenReturn { "2" }
        }

        val exp: Expression<String> = mock()
        assertEquals("1", ComposedInterpreter(listOf(i, i2)).eval(exp).get())
        assertEquals("1", ComposedInterpreter(listOf(i2, i)).eval(exp).get())
    }

    @Test
    fun `test no interpreter found`() {
        val logger: (ExpressionError) -> Unit = mock()
        val exp: Expression<String> = mock()
        assertEquals(Result.error(ExpressionError.NoInterpreterFound(exp)), ComposedInterpreter(emptyList(), logger).eval(exp))
        verify(logger).invoke(ExpressionError.NoInterpreterFound(exp))
        verifyNoMoreInteractions(logger)
    }

    @Test
    fun `test eval error`() {
        val logger: (ExpressionError) -> Unit = mock()
        val partlyInterpreter: PartlyInterpreter = mock()
        val exp: Expression<String> = mock()
        val interpreter = ComposedInterpreter(listOf(partlyInterpreter), logger)
        val exception = RuntimeException()
        whenever(partlyInterpreter.eval(exp, interpreter)).thenThrow(exception)

        assertEquals(Result.error(ExpressionError.EvalError(exp, exception)), interpreter.eval(exp))
        verify(logger).invoke(ExpressionError.EvalError(exp, exception))
        verifyNoMoreInteractions(logger)
    }

    @Test
    fun `test invoke error`() {
        val logger: (ExpressionError) -> Unit = mock()
        val partlyInterpreter: PartlyInterpreter = mock()
        val exp: Expression<String> = mock()
        val interpreter = ComposedInterpreter(listOf(partlyInterpreter), logger)
        val exception = RuntimeException()
        whenever(partlyInterpreter.eval(exp, interpreter)).thenReturn({ throw exception })

        assertEquals(Result.error(ExpressionError.InvokeError(exp, exception)), interpreter.eval(exp))
        verify(logger).invoke(ExpressionError.InvokeError(exp, exception))
        verifyNoMoreInteractions(logger)
    }

    @Test
    fun `test expression eval`() {
        val interpreter: Interpreter = mock()
        val exp: Expression<String> = mock()
        val result = Result.Companion.of("")
        whenever(interpreter.eval(exp)).thenReturn(result)
        assertEquals(result, exp.eval(interpreter))
    }
}