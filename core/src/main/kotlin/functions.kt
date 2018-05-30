package bob.free

class FunctionExpression<out O : Any>(internal val f: Interpreter.() -> O) : Expression<O>

object FunctionInterpreter : PartlyInterpreter {
    override val priority: Int
        get() = 0

    override fun <O : Any> eval(exp: Expression<O>, interpreter: Interpreter) =
            (exp as? FunctionExpression<O>)?.let {
                { exp.f.invoke(interpreter) }
            }
}

fun <O : Any> expr(f: Interpreter.() -> O) = FunctionExpression(f)

fun <A : Any, B : Any> Expression<A>.map(f: (A) -> B): Expression<B> =
        let { a ->
            expr {
                eval(a).map(f).getOrElse { throw it }
            }
        }

fun <A : Any, B : Any> Expression<A>.flatMap(f: (A) -> Expression<B>): Expression<B> =
        let { a ->
            expr { eval(a).flatMap { eval(f(it)) }.getOrElse { throw it } }
        }

fun <A : Any, B : Any> Expression<A>.mapResult(f: (Result<A>) -> B): Expression<B> =
        let { a ->
            expr { f(eval(a)) }
        }

fun <A : Any, B : Any> Expression<A>.flatMapResult(f: (Result<A>) -> Expression<B>): Expression<B> =
        let { a ->
            expr { eval(f(eval(a))).getOrElse { throw it } }
        }

val functionInterpreter = ComposedInterpreter(listOf(FunctionInterpreter))
