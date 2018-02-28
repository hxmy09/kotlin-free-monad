% Android Monad
% Bob Qi
% Feb 28, 2018

## Build Programming Language based on our Domain Logic ##

* Kotlin Embedded

* Define our own syntax and expressions

* Exceptions safe

* Can change implementation at last minute


## How ##

* Return Expressions instead of values in our API functions, and eval these expressions when needed.
* Usually we only need eval expressions in Android life-cycle/callback methods.
* We can define different Interpreters for different purpose.

``` kotlin
interface Expression<out V : Any>
interface Interpreter {
    fun <O : Any> eval(exp: Expression<O>): Result<O, Exception>

    val logger: (ExpressionError) -> Unit
        get() = { }
}

// in Android fragments/activities
expression.eval(interpreter)
```

## Expressions composition ##

``` kotlin
fun <O : Any> expr(f: Interpreter.() -> O): Expressions<O>
fun <A : Any, B : Any> Expression<A>.map(f: (A) -> B): Expression<B>
fun <A : Any, B : Any> Expression<A>.flatMap(f: (A) -> Expression<B>): Expression<B>
fun <A : Any, B : Any> Expression<A>.mapResult(f: (Result<A, Exception>) -> B): Expression<B>
fun <A : Any, B : Any> Expression<A>.flatMapResult(f: (Result<A, Exception>) -> Expression<B>): Expression<B>

UserStateUseCase.GetUserState.map(updater)
        .flatMap {
            UserStateUseCase.UpdateUserState(it)
        }
        .flatMapResult {
            expr {
                it.fold({ onSuccess() }, onError)
            }
        }
```

## Exceptions safe ##

All expressions results are wrap into Result<V, Exceptions> instances, no exception is thrown. We can program for success case or failure case for the results.
All exceptions are logged automatically when evaluating expressions.

``` kotlin
    fun <O : Any> eval(exp: Expression<O>): Result<O, Exception>

    Result.of(operation)
      .flatMap { normalizedData(it) }
      .map { createRequestFromData(it) }
      .flatMap { database.updateFromRequest(it) }

    //success
    result.success {
    }

    //failure
    result.failure {
    }

    //fold is there, if you want to handle both success and failure
    result.fold({ value ->
    //do something with value
    }, { error ->
    //do something with error
    })
```

## Railway oriented programming ##

![diagram](https://fsharpforfunandprofit.com/assets/img/Recipe_Railway_Transparent.png)

What we want to do is connect the Success output of one to the input of the next, but somehow bypass the second function in case of a Failure output.

[[https://fsharpforfunandprofit.com/posts/recipe-part2/]]

## Concurrency built-in ##

``` kotlin
interface JobControl<out T : Any> {
    fun cancel(): Boolean
    fun await(): Result<T, Exception>
}
fun <O : Any> Expression<O>.async():Expression<JobControl<O>>
fun <O : Any> Expression<O>.runOnMainThread(): Expression<JobControl<O>>

operation.flatMap{
    callback(it).runOnMainThread()
}.async()
```

## Add expressions ##

* Use cases

``` kotlin
sealed class UserStateUseCase<out O : Any> : Expression<O> {
    object SyncUserState : UserStateUseCase<Unit>()
    object GetUserState : UserStateUseCase<UserState>()
    data class UpdateUserState(val state: UserState, val syncToRemote: Boolean = true) : UserStateUseCase<Unit>()
}

class UserStateInterpreter(private val repository: UserStateRepository) : PartlyInterpreter {
    override fun <O : Any> eval(exp: Expression<O>, interpreter: Interpreter) =
            (exp as? UserStateUseCase<O>)?.let {
              TODO()
            }
    }
```

* Dependencies like cache/network

``` kotlin
data class GetCacheExpression(val name: String) : Expression<Cache>
class ServiceExpression<S : Any>(val serviceType: Class<S>) : Expression<S>
```

* UI logic, like Navigations etc.

``` kotlin
data class GotoBranchLocator() : Expression<Unit>
```

## Change implementation ##

* Logic in interpreters

``` kotlin
// in eval method of a PartlyInterpreter instance
if (toggleOn) expImpl1() else expImpl2()
```

* Change another interpreter

``` kotlin
exp.eval(if (toggleOn) interpreter1 else interpreter2)
```
