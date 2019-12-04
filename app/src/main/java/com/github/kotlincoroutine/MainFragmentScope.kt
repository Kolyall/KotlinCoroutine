package com.github.kotlincoroutine

import com.github.kotlincoroutine.api.ApiService
import com.github.kotlincoroutine.base.BaseFragmentScope
import com.github.kotlincoroutine.usecases.LoadPlaceUseCase
import com.github.kotlincoroutine.usecases.LoadUsersUseCase
import com.github.kotlincoroutine.usecases.models.User
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class MainFragmentScope : BaseFragmentScope() {

    private var apiService: ApiService = ApiService()
    private var loadUsersUseCase: LoadUsersUseCase = LoadUsersUseCase()
    private var loadPlaceUseCase: LoadPlaceUseCase = LoadPlaceUseCase()
    lateinit var view: MainFragmentView

    fun load() {
//        useCaseRxJavaStyle()
//        launchHandleCatch()

//        flow {
//            for (i in 0..10) {
//                emit(i)
//            }
//        }

        //        (0..10).asFlow()

//        flowOf(1)

        val userProvider: () -> User = {
            User("UserId")
        }

        userProvider
            .asFlow()
            .onEach {
                delay(2000)
            }
            .progressOn(view)
            .launchOnMain(this) {
                view.showText("Value ${it.id}")
            }

    }

    /*solution launch handle catch*/
    private fun launchHandleCatch() {
        launch {
            runCatching {
                withContext(Dispatchers.Main) {
                    showProgress()
                    withContext(Dispatchers.IO) {
                        delay(1000)
                        throw IllegalArgumentException("IllegalArgumentException")
                        view.showText("loadData2")
                    }
                    hideProgress()
                }
            }.onFailure {
                hideProgress()
                view.showText(it.message)
            }
        }
    }

    /*use case solution*/
    private fun useCaseRxJavaStyle() {
        loadPlaceUseCase.setup()
            .doOnSubscribe { showProgress() }
            .doOnComplete {
                hideProgress()
                view.showText("PlacesList " + it.map { place -> place.id }.reduce { t1, t2 -> "$t1, $t2" })
            }
            .doOnError { hideProgress() }
            .execute()
    }

    //    async-await
    private fun loadData1(doOnResult: () -> Unit) {
        launch {
            showProgress()
            try {
                val result = coroutineScope {
                    async {
                        delay(1000)
                        return@async "loadData1"
                    }.await()
                }
                view.showText(result)
            } catch (exception: Exception) {
                view.showText("Error1")
            }
            hideProgress()
            delay(1000)
            doOnResult()
        }
    }

    //    withContext
    private fun loadData2(doOnResult: () -> Unit) {
        launch {
            withContext(Dispatchers.Main) {
                showProgress()
                withContext(Dispatchers.IO) {
                    delay(1000)
                    try {
                        view.showText("loadData2")
                    } catch (exception: Exception) {
                        view.showText("Error2")
                    }
                }
                hideProgress()
                withContext(Dispatchers.IO) {
                    delay(1000)
                }
                doOnResult()
            }
        }
    }

    //    withContext-exceptionHandler
    private fun loadData22(doOnResult: () -> Unit) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            view.showText(throwable.message ?: "")
            hideProgress()
            launch {
                withContext(Dispatchers.Main) {
                    withContext(Dispatchers.IO) {
                        delay(1000)
                    }
                    doOnResult()
                }
            }
        }
        launch(exceptionHandler) {
            withContext(Dispatchers.Main) {
                showProgress()
                withContext(Dispatchers.IO) {
                    delay(1000)
                    throw IllegalArgumentException("IllegalArgumentException loadData22")
                }
                hideProgress()
                withContext(Dispatchers.IO) {
                    delay(1000)
                }
                doOnResult()
            }
        }
    }

    //    dont use launch
    //    launch
    private fun loadData3(doOnResult: () -> Unit) {
        launch {
            showProgress()
            delay(1000)
            try {
                view.showText("loadData3")
            } catch (exception: Exception) {
                view.showText("Error3")
            }
            hideProgress()
            delay(1000)
            doOnResult()
        }
    }

    //    dont use launch
    //    launch-exceptionHandler
    private fun loadData4(doOnResult: () -> Unit) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            view.showText(throwable.message ?: "")
            hideProgress()
            launch {
                delay(1000)
                doOnResult()
            }
        }
        launch(exceptionHandler) {
            showProgress()
            delay(1000)
            throw IllegalArgumentException("My IllegalArgumentException")
            view.showText("loadData4")
            hideProgress()
            delay(1000)
            doOnResult()
        }
    }

    private suspend fun doLoadData() {
        withContext(Dispatchers.Main) {
            showProgress()
            withContext(Dispatchers.IO) {
                apiService.loadTitleData()
                    .withResult(
                        doOnSuccess = {
                            view.showText(it)
                        },
                        doOnError = {
                            view.showText("Error")
                        }
                    )
            }
            hideProgress()
        }
    }

    private fun showProgress() {
        view.showText("")
        view.showProgress()
    }

    private fun hideProgress() {
        view.hideProgress()
    }

}

private fun <T> Flow<T>.progressOn(view: HasProgress): Flow<T> {
    return onStartAtMain { view.showProgress() }
        .onCompletionAtMain { view.hideProgress() }
}

//private fun <T> Flow<T>.launchIn(context: CoroutineContext, function: (List<T>) -> Unit) {
//    collect { result ->
//        withContext(context) {
//            function(result)
//        }
//    }
//}

public fun <T> Flow<T>.launchOn(scope: CoroutineScope, context: CoroutineContext, action: (T) -> Unit): Job {
    return scope.launch {
        collect { result ->
            withContext(context) {
                action(result)
            }
        } // tail-call
    }
}

public fun <T> Flow<T>.launchOnMain(scope: CoroutineScope, action: (T) -> Unit): Job {
    return scope.launch {
        collect { result ->
            withContext(Dispatchers.Main) {
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
    action: suspend () -> Unit
): Flow<T> {
    return onStart {
        withContext(Dispatchers.Main) {
            action()
        }
    }
}
fun <T> Flow<T>.onCompletionAtMain(
    action: suspend () -> Unit
): Flow<T> {
    return onCompletion {
        withContext(Dispatchers.Main) {
            action()
        }
    }
}

