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

    fun <T> then(callback: (S) -> T): Promise<T> {
        return Promise { nextResolve, nextRejected ->
            val resolvedHandler: (S) -> Unit = {
                try {
                    nextResolve(callback(it))
                } catch (e: Throwable) {
                    nextRejected(e)
                }
            }

            when (state) {
                State.FULFILLED -> resolvedHandler(resolvedValue!!)
                State.REJECTED -> nextRejected(rejectedError!!)
                else -> {
                    onResolvedHandlers.add(resolvedHandler)
                    onRejectedHandlers.add { nextRejected(it) }
                }
            }
        }
    }

    fun <T> catch(callback: (Throwable) -> T): Promise<Either<S, T>> {
        return Promise { nextResolve, nextRejected ->
            val rejectedHandler: (Throwable) -> Unit = {
                try {
                    nextResolve(Either.Rejected(callback(it)))
                } catch (e: Throwable) {
                    nextRejected(e)
                }
            }

            when (state) {
                State.FULFILLED -> nextResolve(Either.Resolved(resolvedValue!!))
                State.REJECTED -> rejectedHandler(rejectedError!!)
                else -> {
                    onResolvedHandlers.add { nextResolve(Either.Resolved(it)) }
                    onRejectedHandlers.add(rejectedHandler)
                }
            }
        }
    }

    fun finally(callback: () -> Unit): Promise<S> {
        return Promise { nextResolve, nextReject ->
            fun <T> runFinallyAndPass(value: T): Promise<T> {
                return try {
                    Promise.resolve(callback()).then { value }
                } catch (e: Throwable) {
                    Promise.reject(e)
                }
            }

            this.then { value ->
                runFinallyAndPass(value).then(nextResolve).catch(nextReject)
            }.catch { error ->
                runFinallyAndPass(Unit).then { throw error }
                    .catch { newError -> nextReject(newError) }
            }
        }
    }


    companion object {
        fun <S> resolve(value: S): Promise<S> = Promise { res, _ -> res(value) }
        fun <S> reject(error: Throwable): Promise<S> =
            Promise { _, rej -> rej(error) }
    }
}