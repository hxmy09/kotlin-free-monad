package bob.free

import arrow.core.Try
import arrow.core.recoverWith
import arrow.core.transform

interface Expression<out V : Any>

sealed class ExpressionError : RuntimeException() {
    data class NoInterpreterFound(val expression: Expression<*>) : ExpressionError()
    data class EvalError(val exception: Throwable) : ExpressionError()
    data class InvokeError(val exception: Throwable) : ExpressionError()
}

typealias Result<O> = Try<O>
typealias Success<O> = Try.Success<O>
typealias Failure<O> = Try.Failure<O>

inline fun <O> Result<O>.getOrElse(default: (Throwable) -> O): O = fold(default, { it })

interface Interpreter {
    fun <O : Any> eval(exp: Expression<O>): Result<O>

    val logger: (Throwable) -> Unit
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


class ComposedInterpreter(interpreters: List<PartlyInterpreter>, override val logger: (Throwable) -> Unit = {}) : Interpreter {
    private val partlyInterpreters: List<PartlyInterpreter> =
            interpreters.toSet().sortedBy { it.priority }

    override fun <O : Any> eval(exp: Expression<O>): Result<O> =
            Result {
                partlyInterpreters.fold(null as (() -> O)?) { a, i ->
                    a ?: i.eval(exp, this)
                }
                        ?: throw ExpressionError.NoInterpreterFound(exp).also(logger)
            }.transform({
                Result { it() }
                        .recoverWith {
                            Failure(it as? ExpressionError
                                    ?: ExpressionError.InvokeError(it).also { logger(it.exception) })
                        }
            }) {
                Failure(it as? ExpressionError
                        ?: ExpressionError.EvalError(it).also { logger(it.exception) })
            }
}
