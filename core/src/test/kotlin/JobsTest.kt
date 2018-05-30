import bob.free.*
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JobsTest {

    @Before
    fun setup() {
        CoroutinesExpressionJob.uiDispatcher = lazy { Unconfined }
    }

    @Test
    fun cancel() {
        val i: Interpreter = mockk(relaxed = true)
        val f: (String) -> String = mockk(relaxed = true)
        every { (i.eval<String>(any())) } answers { Result { Thread.sleep(100L); f("") } }
        CoroutinesExpressionJob(i, mockk<Expression<String>>(relaxed = true)).let {
            it.cancel()
            runBlocking(CoroutinesExpressionJob.defaultDispatcher) { delay(100L) }
            verify { f wasNot called }
        }
    }

    @Test
    fun await() {
        val i: Interpreter = mockk()
        val exp: Expression<String> = mockk()
        every { i.eval(exp) } returns (Success("result"))

        assertTrue(CoroutinesExpressionJob(i, exp).await().exists { it == "result" })
    }

    @Test(expected = NoClassDefFoundError::class)
    fun `test mainThread eval with default context extension`() {
        CoroutinesExpressionJob.uiDispatcher = lazy { UI }
        FunctionInterpreter.eval(mockk<Expression<String>>().runOnMainThread(), mockk())?.invoke()
    }

    @Test
    fun `test expression async extension`() {
        val exp: Expression<String> = mockk(relaxed = true)
        val i: Interpreter = mockk(relaxed = true)
        FunctionInterpreter.eval(exp.async(), i)?.invoke()?.await()
        verify { i.eval(exp) }
    }

    @Test
    fun `test expression main thread extension`() {
        val exp: Expression<String> = mockk(relaxed = true)
        val i: Interpreter = mockk(relaxed = true)
        FunctionInterpreter.eval(exp.runOnMainThread(), i)?.invoke()
        runBlocking(CoroutinesExpressionJob.uiDispatcher.value) { }
        verify { i.eval(exp) }
    }
}