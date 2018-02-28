package bob.free
import com.github.kittinunf.result.Result
import com.nhaarman.mockito_kotlin.*
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JobsTest {

    @Before
    fun setup() {
        CoroutinesExpressionJob.uiDispatcher = lazy { Unconfined }
    }

    @Test
    fun cancel() {
        val i: Interpreter = mock()
        val f: (String) -> String = mock()
        whenever(i.eval<String>(any())).then { Thread.sleep(1000L); f }
        CoroutinesExpressionJob(i, mock<Expression<String>>()).cancel()
        verifyZeroInteractions(f)
    }

    @Test
    fun await() {
        val i: Interpreter = mock()
        val exp: Expression<String> = mock()
        whenever(i.eval(exp)).thenReturn(Result.of("result"))

        assertEquals("result", CoroutinesExpressionJob(i, exp).await().get())
    }

    @Test(expected = NoClassDefFoundError::class)
    fun `test mainThread eval with default context extension`() {
        CoroutinesExpressionJob.uiDispatcher = lazy { UI }
        FunctionInterpreter.eval(mock<Expression<String>>().runOnMainThread(), mock())?.invoke()
    }

    @Test
    fun `test expression async extension`() {
        val exp: Expression<String> = mock()
        val i: Interpreter = mock()
        FunctionInterpreter.eval(exp.async(), i)?.invoke()?.await()
        verify(i).eval(exp)
    }

    @Test
    fun `test expression main thread extension`() {
        val exp: Expression<String> = mock()
        val i: Interpreter = mock()
        FunctionInterpreter.eval(exp.runOnMainThread(), i)?.invoke()
        runBlocking(CoroutinesExpressionJob.uiDispatcher.value) { }
        verify(i).eval(exp)
    }
}