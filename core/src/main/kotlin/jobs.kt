package bob.free

import com.github.kittinunf.result.Result
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlin.coroutines.experimental.CoroutineContext

/**
 * @author a483334
 * @since 2/5/18
 */
interface JobControl<out T : Any> {
    fun cancel(): Boolean
    fun await(): Result<T, Exception>
}

class CoroutinesExpressionJob<out O : Any>(interpreter: Interpreter,
                                           expression: Expression<O>,
                                           context: CoroutineContext = DefaultDispatcher)
    : JobControl<O> {
    private val job = async(context) {
        takeIf { isActive }
                ?.let { interpreter.eval(expression) }
                .takeIf { isActive }
                ?: Result.error(ExpressionError.InvokeError(expression, CancellationException()))
    }

    override fun cancel() = job.cancel()

    override fun await(): Result<O, Exception> = kotlinx.coroutines.experimental.runBlocking {
        job.await()
    }

    companion object {
        var uiDispatcher: Lazy<CoroutineContext> = lazy { UI }
    }
}

fun <O : Any> Expression<O>.async(): Expression<JobControl<O>> = let { exp ->
    expr { CoroutinesExpressionJob(this, exp) }
}

fun <O : Any> Expression<O>.runOnMainThread(): Expression<JobControl<O>> = let { exp ->
    expr { CoroutinesExpressionJob(this, exp, CoroutinesExpressionJob.uiDispatcher.value) }
}
