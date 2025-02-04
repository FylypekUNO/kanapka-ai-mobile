package pl.fylypek.kanapka_ai_mobile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

enum class State {
    PENDING,
    FULFILLED,
    REJECTED
}

sealed interface Either<L, R> {
    class Resolved<L, R>(val value: L) : Either<L, R>
    class Rejected<L, R>(val value: R) : Either<L, R>
}

class Promise<S> {
    private val coroutineContext: CoroutineContext = Dispatchers.Default

    private var state: State = State.PENDING
    private var resolvedValue: S? = null
    private var rejectedError: Throwable? = null

    private val onResolvedHandlers: MutableList<(S) -> Unit> = mutableListOf()
    private val onRejectedHandlers: MutableList<(Throwable) -> Unit> = mutableListOf()

    constructor(executor: (resolve: (S) -> Unit, reject: (Throwable) -> Unit) -> Unit) {
        CoroutineScope(coroutineContext).launch {
            try {
                executor(::resolve, ::reject)
            } catch (e: Throwable) {
                reject(e)
            }
        }
    }

    private fun resolve(value: S) {
        if (state != State.PENDING) return
        state = State.FULFILLED

        resolvedValue = value

        onResolvedHandlers.forEach { it.invoke(value) }
    }

    private fun reject(error: Throwable) {
        if (state != State.PENDING) return
        state = State.REJECTED

        rejectedError = error

        if (onRejectedHandlers.isNotEmpty()) {
            onRejectedHandlers.forEach { it.invoke(error) }
        } else {
            println("Unhandled promise rejection: $error")
        }
    }

    private fun <T> thenFulfilled(callback: (S) -> T): Promise<T> {
        return Promise { nextResolve, nextRejected ->
            try {
                nextResolve(callback(resolvedValue!!))
            } catch (e: Throwable) {
                nextRejected(e)
            }
        }
    }

    private fun <T> thenRejected(callback: (S) -> T): Promise<T> {
        return Promise { nextResolve, nextRejected ->
            nextRejected(rejectedError!!)
        }
    }

    private fun <T> thenPending(callback: (S) -> T): Promise<T> {
        return Promise { nextResolve, nextRejected ->
            onResolvedHandlers.add { value ->
                var newValue: T? = null

                try {
                    newValue = callback(value)
                } catch (e: Throwable) {
                    nextRejected(e)
                }

                nextResolve(newValue!!)
            }
            onRejectedHandlers.add { error ->
                nextRejected(error)
            }
        }
    }

    fun <T> then(callback: (S) -> T): Promise<T> {
        return when (state) {
            State.FULFILLED -> thenFulfilled(callback)
            State.REJECTED -> thenRejected(callback)
            else -> thenPending(callback)
        }
    }

    private fun <T> catchFulfilled(callback: (Throwable) -> T): Promise<Either<S, T>> {
        return Promise { nextResolve, _ ->
            nextResolve(Either.Resolved(resolvedValue!!))
        }
    }

    private fun <T> catchRejected(callback: (Throwable) -> T): Promise<Either<S, T>> {
        return Promise { nextResolve, nextRejected ->
            try {
                nextResolve(Either.Rejected(callback(rejectedError!!)))
            } catch (e: Throwable) {
                nextRejected(e)
            }
        }
    }

    private fun <T> catchPending(callback: (Throwable) -> T): Promise<Either<S, T>> {
        return Promise { nextResolve, nextRejected ->
            onResolvedHandlers.add { value ->
                nextResolve(Either.Resolved(value))
            }
            onRejectedHandlers.add { error ->
                try {
                    nextResolve(Either.Rejected(callback(error)))
                } catch (e: Throwable) {
                    nextRejected(e)
                }
            }
        }
    }

    fun <T> catch(callback: (Throwable) -> T): Promise<Either<S, T>> {
        return when (state) {
            State.FULFILLED -> catchFulfilled(callback)
            State.REJECTED  -> catchRejected(callback)
            else            -> catchPending(callback)
        }
    }

}