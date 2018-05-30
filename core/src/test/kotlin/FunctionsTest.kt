import bob.free.*
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test

class FunctionsTest {
    @Test
    fun `test function interpreter`() {
        val exp: Expression<String> = mockk(relaxed = true)
        assertNull(FunctionInterpreter.eval(exp, mockk(relaxed = true)))

        val fExpression = FunctionExpression<String>(mockk<(Interpreter) -> String>().also {
            every { it.invoke(any()) } returns "a"
        })
        val i: Interpreter = mockk(relaxed = true)
        val f = FunctionInterpreter.eval(fExpression, i)
        assertNotNull(f)

        f?.invoke()
        verify { fExpression.f.invoke(i) }
    }

    @Test
    fun `test expr extension`() {
        val f: Interpreter.() -> String = mockk(relaxed = true)
        val exp = expr(f)
        assertEquals(f, exp.f)
    }

    @Test
    fun `test map`() {
        val a: Expression<String> = mockk(relaxed = true)
        val b: (String) -> String = mockk(relaxed = true)
        val mapped = a.map(b)
        assertTrue(mapped is FunctionExpression<String>)
        (mapped as FunctionExpression<String>)
            .let {
                val partlyInterpreter: PartlyInterpreter = mockk(relaxed = true)
                val interpreter =
                    ComposedInterpreter(listOf(FunctionInterpreter, partlyInterpreter))
                every { partlyInterpreter.eval(a, interpreter) } returns { "a" }
                every { b.invoke(any()) } returns "b"
                assertTrue(interpreter.eval(mapped).exists { it == "b" })

                clearMocks(b)
                val exception = ExpressionError.InvokeError(RuntimeException())
                every { partlyInterpreter.eval(a, interpreter) } throws exception
                assertEquals(Failure<String>(exception), interpreter.eval(mapped))
                verify { b wasNot called }
            }
    }

    @Test
    fun `test flatMap`() {
        val a: Expression<String> = mockk(relaxed = true)
        val b: (String) -> Expression<String> = mockk(relaxed = true)
        val mapped = a.flatMap(b)
        assertTrue(mapped is FunctionExpression<String>)
        (mapped as FunctionExpression<String>)
            .let {
                val partlyInterpreter: PartlyInterpreter = mockk(relaxed = true)
                val interpreter =
                    ComposedInterpreter(listOf(FunctionInterpreter, partlyInterpreter))
                val bExp: Expression<String> = mockk(relaxed = true)
                every { partlyInterpreter.eval(a, interpreter) } returns { "a" }
                every { partlyInterpreter.eval(bExp, interpreter) } returns { "b" }
                every { b.invoke(any()) } returns bExp
                assertTrue(interpreter.eval(mapped).exists { it == "b" })

                clearMocks(b, bExp)
                val exception = ExpressionError.InvokeError(RuntimeException())
                every { partlyInterpreter.eval(a, interpreter) } throws exception
                assertEquals(Failure<String>(exception), interpreter.eval(mapped))
                verify {
                    b wasNot called
                    bExp wasNot called
                }
            }
    }

    @Test
    fun `test mapResult`() {
        val a: Expression<String> = mockk(relaxed = true)
        val b: (Result<String>) -> String = mockk(relaxed = true)
        val mapped = a.mapResult(b)
        assertTrue(mapped is FunctionExpression<String>)
        (mapped as FunctionExpression<String>)
            .let {
                val partlyInterpreter: PartlyInterpreter = mockk(relaxed = true)
                val interpreter =
                    ComposedInterpreter(listOf(FunctionInterpreter, partlyInterpreter))
                every { partlyInterpreter.eval(a, interpreter) } returns { "a" }
                every { b.invoke(Success("a")) } returns "b"
                assertTrue(interpreter.eval(mapped).exists { it == "b" })

                val exception = ExpressionError.InvokeError(RuntimeException())
                every { partlyInterpreter.eval(a, interpreter) } throws exception
                every { b.invoke(Failure(exception)) } returns "exception"
                assertTrue(interpreter.eval(mapped).exists { it == "exception" })

                val invokeException = RuntimeException()
                every { b.invoke(any()) } throws invokeException
                assertTrue(interpreter.eval(mapped).failed().exists {
                    (it as? ExpressionError.InvokeError)?.exception == invokeException
                })
            }
    }

    @Test
    fun `test flatMapResult`() {
        val a: Expression<String> = mockk(relaxed = true)
        val b: (Result<String>) -> Expression<String> = mockk(relaxed = true)
        val mapped = a.flatMapResult(b)
        assertTrue(mapped is FunctionExpression<String>)
        (mapped as FunctionExpression<String>)
            .let {
                val partlyInterpreter: PartlyInterpreter = mockk(relaxed = true)
                val interpreter =
                    ComposedInterpreter(listOf(FunctionInterpreter, partlyInterpreter))
                val bExp: Expression<String> = mockk(relaxed = true)
                every { partlyInterpreter.eval(a, interpreter) } returns { "a" }
                every { partlyInterpreter.eval(bExp, interpreter) } returns { "b" }
                every { b.invoke(any()) } returns bExp
                assertTrue(interpreter.eval(mapped).exists { it == "b" })

                val exception = ExpressionError.InvokeError(RuntimeException())
                every { partlyInterpreter.eval(a, interpreter) } throws exception
                assertTrue(interpreter.eval(mapped).exists { it == "b" })

                val e = RuntimeException()
                every { b.invoke(any()) } throws ExpressionError.EvalError(e)
                assertEquals(
                    Failure<String>(ExpressionError.EvalError(e)),
                    interpreter.eval(mapped)
                )
            }
    }

    @Test
    fun `test functionInterpreter`() {
        assertEquals(Result { 2 }, functionInterpreter.eval(expr { 2 }))
        assertFalse(functionInterpreter.eval(mockk<Expression<String>>()).exists { true })
    }
}