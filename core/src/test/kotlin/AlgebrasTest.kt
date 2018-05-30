import bob.free.*
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgebrasTest {
    @Test
    fun `test interpreters compose`() {
        val partlyInterpreters =
            listOf(mockk<PartlyInterpreter>(relaxed = true), mockk(relaxed = true))
        val interpreter = ComposedInterpreter(partlyInterpreters)
        clearMocks(*partlyInterpreters.toTypedArray())

        val exp: Expression<String> = mockk(relaxed = true)
        every { partlyInterpreters[0].eval(exp, interpreter) } returns { "eval1" }
        assertTrue(interpreter.eval(exp).exists { it == "eval1" })
        verify { partlyInterpreters[1] wasNot called }

        clearMocks(partlyInterpreters[0])
        every { partlyInterpreters[0].eval(exp, interpreter) } returns null
        every { partlyInterpreters[1].eval(exp, interpreter) } returns { "eval2" }
        assertTrue(interpreter.eval(exp).exists { it == "eval2" })
        verify { partlyInterpreters[0].eval(exp, interpreter) }
    }

    @Test
    fun `test interpreters compose order`() {
        val i = mockk<PartlyInterpreter>().also {
            every { it.priority } returns 1
            every { it.eval<String>(any(), any()) } returns { "1" }
        }
        val i2 = mockk<PartlyInterpreter>().also {
            every { it.priority } returns 2
            every { it.eval<String>(any(), any()) } returns { "2" }
        }

        val exp: Expression<String> = mockk()
        assertTrue(ComposedInterpreter(listOf(i, i2)).eval(exp).exists { it == "1" })
        assertTrue(ComposedInterpreter(listOf(i2, i)).eval(exp).exists { it == "1" })
    }

    @Test
    fun `test no interpreter found`() {
        val logger: (Throwable) -> Unit = mockk(relaxed = true)
        val exp: Expression<String> = mockk(relaxed = true)
        assertEquals(
            Failure<String>(ExpressionError.NoInterpreterFound(exp)),
            ComposedInterpreter(emptyList(), logger).eval(exp)
        )
        verifyAll {
            logger.invoke(ExpressionError.NoInterpreterFound(exp))
        }
    }

    @Test
    fun `test eval error`() {
        val logger: (Throwable) -> Unit = mockk(relaxed = true)
        val partlyInterpreter: PartlyInterpreter = mockk(relaxed = true)
        val exp: Expression<String> = mockk(relaxed = true)
        val interpreter = ComposedInterpreter(listOf(partlyInterpreter), logger)
        val exception = RuntimeException()
        every { partlyInterpreter.eval(exp, interpreter) } throws exception

        assertEquals(
            Failure<String>(ExpressionError.EvalError(exception)),
            interpreter.eval(exp)
        )
        verifyAll { logger.invoke(exception) }
    }

    @Test
    fun `test invoke error`() {
        val logger: (Throwable) -> Unit = mockk(relaxed = true)
        val partlyInterpreter: PartlyInterpreter = mockk(relaxed = true)
        val exp: Expression<String> = mockk(relaxed = true)
        val interpreter = ComposedInterpreter(listOf(partlyInterpreter), logger)
        val exception = RuntimeException()
        every { partlyInterpreter.eval(exp, interpreter) } returns { throw exception }

        assertEquals(
            Failure<String>(ExpressionError.InvokeError(exception)),
            interpreter.eval(exp)
        )
        verifyAll { logger.invoke(exception) }
    }

    @Test
    fun `test expression eval`() {
        val interpreter: Interpreter = mockk()
        val exp: Expression<String> = mockk()
        val result = Success("")
        every { interpreter.eval(exp) } returns result
        assertEquals(result, exp.eval(interpreter))
    }
}
