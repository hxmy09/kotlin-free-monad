package bob.free

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlin.coroutines.experimental.CoroutineContext

interface JobControl<out T : Any> {
    fun cancel(): Boolean
    fun await(): Result<T>
}

class CoroutinesExpressionJob<out O : Any>(
    interpreter: Interpreter,
    expression: Expression<O>,
    context: CoroutineContext = CoroutinesExpressionJob.defaultDispatcher
) : JobControl<O> {
    private val job = async(context) {
        takeIf { isActive }
            ?.let { interpreter.eval(expression) }
            .takeIf { isActive }
                ?: Failure(ExpressionError.InvokeError(CancellationException()))
    }

    override fun cancel() = job.cancel()

    override fun await(): Result<O> = kotlinx.coroutines.experimental.runBlocking {
        job.await()
    }

    companion object {
        var uiDispatcher: Lazy<CoroutineContext> = lazy { UI }
        var defaultDispatcher = DefaultDispatcher
    }
}

fun <O : Any> Expression<O>.async(parent: Job? = null): Expression<JobControl<O>> = let { exp ->
    expr {
        CoroutinesExpressionJob(
            this,
            exp,
            if (parent != null) parent + DefaultDispatcher else DefaultDispatcher
        )
    }
}

fun <O : Any> Expression<O>.runOnMainThread(parent: Job? = null): Expression<JobControl<O>> =
    let { exp ->
        expr {
            CoroutinesExpressionJob(
                this,
                exp,
                CoroutinesExpressionJob.uiDispatcher.value.let { if (parent != null) parent + it else it })
        }
    }
