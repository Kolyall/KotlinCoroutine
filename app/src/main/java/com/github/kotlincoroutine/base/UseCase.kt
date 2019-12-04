package com.github.kotlincoroutine.base

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

abstract class UseCase<T> : CoroutineScope {

    private var io = Dispatchers.IO
    private var ui = Dispatchers.Main

    private lateinit var supervisorJob: Job

    override val coroutineContext: CoroutineContext
        get() = supervisorJob + ui

    fun setup(): UseCase<T> {
        onCompleteCallbacks.clear()
        onSubscribeCallbacks.clear()
        onErrorCallbacks.clear()
        return this
    }

    fun execute(doOnSubscribe: OnSubscribe = {}, doOnComplete: OnComplete<T> = {}, doOnError: (Throwable) -> Unit = {}): Job {
        onSubscribeCallbacks.add(doOnSubscribe)
        onCompleteCallbacks.add(doOnComplete)
        onErrorCallbacks.add(doOnError)

        supervisorJob = SupervisorJob()
        return launch {
            withContext(ui) {
                executeOnSubscribe()
                try {
                    val result = withContext(io) {
                        doOnBackground()
                    }
                    executeOnComplete(result)
                } catch (exception: CancellationException) {
                    Log.d("UseCase", "canceled by user")
                } catch (exception: Exception) {
                    executeOnError(exception)
                }
            }
        }
    }

    private var onCompleteCallbacks = mutableListOf<OnComplete<T>>()
    private var onSubscribeCallbacks = mutableListOf<OnSubscribe>()
    private var onErrorCallbacks = mutableListOf<OnError>()

    private fun executeOnSubscribe() {
        onSubscribeCallbacks.forEach { it() }
    }

    private fun executeOnComplete(result: T) {
        onCompleteCallbacks.forEach { it(result) }
    }

    private fun executeOnError(exception: Exception) {
        onErrorCallbacks.forEach { it(exception) }
    }

    protected abstract suspend fun doOnBackground(): T

    protected fun <T> doOnBackgroundAsync(context: CoroutineContext = io, block: suspend () -> T): Deferred<T> {
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

    fun doOnSubscribe(doOnSubscribe: OnSubscribe): UseCase<T> {
        onSubscribeCallbacks.add(doOnSubscribe)
        return this
    }
    fun doOnComplete(doOnComplete: OnComplete<T>): UseCase<T> {
        onCompleteCallbacks.add(doOnComplete)
        return this
    }

    fun doOnError(doOnError: OnError): UseCase<T> {
        onErrorCallbacks.add(doOnError)
        return this
    }

}

typealias OnSubscribe = () -> Unit
typealias OnComplete<T> = (T) -> Unit
typealias OnError = (Throwable) -> Unit