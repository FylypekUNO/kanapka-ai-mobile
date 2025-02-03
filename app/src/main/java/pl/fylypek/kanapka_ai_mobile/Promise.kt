package pl.fylypek.kanapka_ai_mobile

import android.os.Handler
import android.os.Looper

class Promise<T> {
    private var onResolve: ((T) -> Unit)? = null
    private var onReject: ((Exception) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    constructor(executor: (resolve: (T) -> Any, reject: (Exception) -> Unit) -> Any?) {
        try {
            Thread {
                executor(::innerResolve, ::innerReject)
            }.start()
        } catch (e: Exception) {
            innerReject(e)
        }
    }

    private fun innerResolve(value: T) {
        mainHandler.post {
            try {
                onResolve?.invoke(value)
            } catch (e: Exception) {
                innerReject(e)
            }
        }
    }

    private fun innerReject(error: Exception) {
        mainHandler.post {
            if (onReject != null) {
                onReject?.invoke(error)
            } else {
                println("Unhandled promise rejection: $error")
            }
        }
    }

    fun <T2> then(callback: (T) -> T2): Promise<T2> {
        val promise = Promise<T2> { resolve, reject ->
            onResolve = {
                try {
                    resolve(callback(it))
                } catch (e: Exception) {
                    reject(e)
                }
            }
            onReject = {
                reject(it)
            }

            return@Promise null
        }

        return promise
    }

    fun <T2> catch(callback: (Exception) -> T2): Promise<Any?> {
        val promise = Promise<Any?> { resolve, reject ->
            onResolve = {
                resolve(it)
            }
            onReject = {
                callback(it)
            }

            return@Promise null
        }

        return promise
    }
}