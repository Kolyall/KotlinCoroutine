package com.github.kotlincoroutine.base

import android.util.Log
import com.github.kotlincoroutine.ext.DispatchersProvider
import com.github.kotlincoroutine.ext.onIo
import com.github.kotlincoroutine.ext.onUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class UseCase<T>(private val dispatchers: DispatchersProvider) : CoroutineScope {

    private lateinit var supervisorJob: Job

    override val coroutineContext: CoroutineContext
        get() {
            return supervisorJob + dispatchers.io()
        }

    fun setup(): UseCase<T> {
        onCompleteCallbacks.clear()
        onSubscribeCallbacks.clear()
        onErrorCallbacks.clear()
        return this
    }

    fun execute(
        dispatchers: DispatchersProvider,
        doOnSubscribe: OnSubscribe = {},
        doOnComplete: OnComplete<T> = {},
        doOnError: (Throwable) -> Unit = {}
    ): Job {
        onSubscribeCallbacks.add(doOnSubscribe)
        onCompleteCallbacks.add(doOnComplete)
        onErrorCallbacks.add(doOnError)

        supervisorJob = SupervisorJob()
        return launch {
            runCatching {
                onUi(dispatchers) {
                    executeOnSubscribe()/*ui*/
                }
                try {
                    val result = onIo(dispatchers) {
                        doOnBackground()/*io*/
                    }
                    onUi(dispatchers) {
                        executeOnComplete(result)/*ui*/
                    }
                } catch (exception: CancellationException) {
                    Log.d("UseCase", "canceled by user")
                } catch (exception: Exception) {
                    onUi(dispatchers) {
                        executeOnError(exception)/*ui*/
                    }
                }
            }.onFailure { exception ->
                onUi(dispatchers) {
                    executeOnError(exception)/*ui*/
                }
            }
        }
    }

    var onCompleteCallbacks = mutableListOf<OnComplete<T>>()
    var onSubscribeCallbacks = mutableListOf<OnSubscribe>()
    var onErrorCallbacks = mutableListOf<OnError>()

    private fun executeOnSubscribe() {
        onSubscribeCallbacks.forEach { it() }
    }

    private fun executeOnComplete(result: T) {
        onCompleteCallbacks.forEach { it(result) }
    }

    private fun executeOnError(exception: Throwable) {
        onErrorCallbacks.forEach { it(exception) }
    }

    protected abstract suspend fun doOnBackground(): T

    protected fun <T> doOnBackgroundAsync(context: CoroutineContext = dispatchers.io(), block: suspend () -> T): Deferred<T> {
        return async(context) {
            block()
        }
    }

    suspend fun <T, R> List<T>.asyncMap(block: suspend (T) -> R): List<R> {
        return this.map {
            doOnBackgroundAsync {
                block(it)
            }
        }.map {
            it.await()
        }
    }

    fun unsubscribe() {
        coroutineContext.cancelChildren()
    }

}

fun <T> UseCase<T>.doOnSubscribe(doOnSubscribe: OnSubscribe): UseCase<T> {
    onSubscribeCallbacks.add(doOnSubscribe)
    return this
}

fun <T> UseCase<T>.doOnSuccess(doOnComplete: OnComplete<T>): UseCase<T> {
    onCompleteCallbacks.add(doOnComplete)
    return this
}

fun <T> UseCase<T>.doOnError(doOnError: OnError): UseCase<T> {
    onErrorCallbacks.add(doOnError)
    return this
}

typealias OnSubscribe = () -> Unit
typealias OnComplete<T> = (T) -> Unit
typealias OnError = (Throwable) -> Unit