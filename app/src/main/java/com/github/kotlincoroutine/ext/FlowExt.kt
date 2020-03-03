package com.github.kotlincoroutine.ext

import com.github.kotlincoroutine.HasProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

fun <T> Flow<T>.progressOn(view: HasProgress,dispatchers: DispatchersProvider): Flow<T> {
    return onStartAtMain(dispatchers) { view.showProgress() }
        .onCompletionAtMain(dispatchers) { view.hideProgress() }
}

//private fun <T> Flow<T>.launchIn(context: CoroutineContext, function: (List<T>) -> Unit) {
//    collect { result ->
//        withContext(context) {
//            function(result)
//        }
//    }
//}

fun <T> Flow<T>.launchOn(scope: CoroutineScope, context: CoroutineContext, action: (T) -> Unit): Job {
    return scope.launch {
        collect { result ->
            withContext(context) {
                action(result)
            }
        } // tail-call
    }
}

fun <T> Flow<T>.launchOnMain(scope: CoroutineScope,dispatchers: DispatchersProvider, action: (T) -> Unit): Job {
    return scope.launch {
        collect { result ->
            onUi(dispatchers) {
                action(result)
            }
        } // tail-call
    }
}

fun <T> Flow<T>.onEachAtMain(action: suspend (T) -> Unit): Flow<T> = transform { value ->
    withContext(Dispatchers.Main) {
        action(value)
    }
    return@transform emit(value)
}

fun <T> Flow<T>.onEachAt(context: CoroutineContext, action: suspend (T) -> Unit): Flow<T> = transform { value ->
    withContext(context) {
        action(value)
    }
    return@transform emit(value)
}

fun <T> Flow<T>.onStartAtMain(
    dispatchers: DispatchersProvider,
    action: suspend () -> Unit
): Flow<T> {
    return onStart {
        onUi(dispatchers) {
            action()
        }
    }
}

fun <T> Flow<T>.onCompletionAtMain(
    dispatchers: DispatchersProvider,
    action: suspend () -> Unit
): Flow<T> {
    return onCompletion {
        onUi(dispatchers) {
            action()
        }
    }
}

suspend fun onUi(dispatchers: DispatchersProvider, function: suspend () -> Unit) {
    withContext(dispatchers.main()) {
        function()
    }
}

suspend fun <T> onIo(dispatchers: DispatchersProvider, function: suspend () -> T): T {
   return withContext(dispatchers.io()) {
         function()
    }
}

class AndroidDispatchersProvider : DispatchersProvider {
    override fun io(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    override fun main(): CoroutineDispatcher {
        return Dispatchers.Main
    }
}

interface DispatchersProvider {
    fun io(): CoroutineDispatcher

    fun main(): CoroutineDispatcher
}
