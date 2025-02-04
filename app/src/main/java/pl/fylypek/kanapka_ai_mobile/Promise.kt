package pl.fylypek.kanapka_ai_mobile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

sealed interface Either<L, R> {
    class Resolved<L, R>(val value: L) : Either<L, R>
    class Rejected<L, R>(val value: R) : Either<L, R>
}

class Promise<S> {
    private val coroutineContext: CoroutineContext = Dispatchers.Default

    private var onResolved: ((S) -> Unit)? = null
    private var onRejected: ((Throwable) -> Unit)? = null

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
        onResolved?.invoke(value)
    }

    private fun reject(error: Throwable) {
        if (onRejected != null) {
            onRejected?.invoke(error)
        } else {
            println("Unhandled promise rejection: $error")
        }
    }

    fun <T> then(callback: (S) -> T): Promise<T> {
        return Promise { nextResolve, nextReject ->
            onResolved = { value ->
                nextResolve(callback(value))
            }
            onRejected = { error ->
                nextReject(error)
            }
        }
    }

    fun <T> catch(callback: (Throwable) -> T): Promise<Either<S, T>> {
        return Promise { nextResolve, _ ->
            onResolved = { value ->
                nextResolve(Either.Resolved(value))
            }
            onRejected = { error ->
                nextResolve(Either.Rejected(callback(error)))
            }

        }
    }
}