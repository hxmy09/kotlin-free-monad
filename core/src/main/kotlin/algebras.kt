package bob.free

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.github.kittinunf.result.mapError

/**
 * @author a483334
 * @since 2/2/18
 */

interface Expression<out V : Any>

sealed class ExpressionError : RuntimeException() {
    data class NoInterpreterFound(val expression: Expression<*>) : ExpressionError()
    data class EvalError(val expression: Expression<*>, val exception: Exception) : ExpressionError()
    data class InvokeError(val expression: Expression<*>, val exception: Exception) : ExpressionError()
}

interface Interpreter {
    fun <O : Any> eval(exp: Expression<O>): Result<O, Exception>

    val logger: (ExpressionError) -> Unit
        get() = { }
}

interface PartlyInterpreter {
    /**
     * the nice value to give other priority, bigger value will let this eval after others
     */
    val priority: Int
        get() = 5

    fun <O : Any> eval(exp: Expression<O>, interpreter: Interpreter): (() -> O)?
}

fun <O : Any> Expression<O>.eval(interpreter: Interpreter) =
        interpreter.eval(this)


class ComposedInterpreter(interpreters: List<PartlyInterpreter>, override val logger: (ExpressionError) -> Unit = {}) : Interpreter {
    private val partlyInterpreters: List<PartlyInterpreter> =
            interpreters.toSet().sortedBy { it.priority }

    override fun <O : Any> eval(exp: Expression<O>): Result<O, Exception> =
            Result.of {
                partlyInterpreters.fold(null as (() -> O)?) { a, i ->
                    a ?: i.eval(exp, this)
                }
                        ?: throw ExpressionError.NoInterpreterFound(exp).also(logger)
            }.mapError {
                        it as? ExpressionError ?: ExpressionError.EvalError(exp, it).also(logger)
                                as Exception // let exception be Exception type so Result.map can throw all exceptions
                    }.map { it() }
                    .mapError {
                        it as? ExpressionError ?: ExpressionError.InvokeError(exp, it).also(logger)
                                as Exception // let exception be Exception type so Result.map can throw all exceptions
                    }
}

