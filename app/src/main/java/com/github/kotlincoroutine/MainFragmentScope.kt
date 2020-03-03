package com.github.kotlincoroutine

import com.github.kotlincoroutine.api.ApiService
import com.github.kotlincoroutine.base.BaseFragmentScope
import com.github.kotlincoroutine.base.doOnError
import com.github.kotlincoroutine.base.doOnSubscribe
import com.github.kotlincoroutine.base.doOnSuccess
import com.github.kotlincoroutine.ext.AndroidDispatchersProvider
import com.github.kotlincoroutine.ext.onIo
import com.github.kotlincoroutine.ext.onUi
import com.github.kotlincoroutine.usecases.LoadPlaceUseCase
import com.github.kotlincoroutine.usecases.LoadUsersUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragmentScope : BaseFragmentScope() {

    val TAG: String = "MainFragmentScope"

    val dispatchers = AndroidDispatchersProvider()

    private var apiService: ApiService = ApiService()
    private var loadUsersUseCase: LoadUsersUseCase = LoadUsersUseCase()
    private var loadPlaceUseCase: LoadPlaceUseCase = LoadPlaceUseCase()
    lateinit var view: MainFragmentView

    fun load() {
        useCaseRxJavaStyle()
//        loadData2 {
//            Log.e(TAG, "load (line 33): ")
//        }
//        launchHandleCatch()

//        flow {
//            for (i in 0..10) {
//                emit(i)
//            }
//        }

        //        (0..10).asFlow()

//        flowOf(1)

//        val userProvider: () -> User = {
//            User("UserId")
//        }
//        userProvider
//            .asFlow()
//            .onEach {
//                delay(2000)
//            }
//            .progressOn(view, dispatchers)
//            .launchOnMain(this, dispatchers) {
//                view.showText("Value ${it.id}")
//            }

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
            .doOnSuccess { hideProgress() }
            .doOnSuccess {
                view.showText("PlacesList " + it.map { place -> place.id }.reduce { t1, t2 -> "$t1, $t2" })
            }
            .doOnError { hideProgress() }
            .execute(dispatchers)
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
            runCatching {
                onUi(dispatchers) {
                    showProgress()
                }
                onIo(dispatchers) {
                    delay(5000)
                    try {
                        view.showText("loadData2")
                    } catch (exception: Exception) {
                        view.showText("Error2")
                    }
                }
                onUi(dispatchers) {
                    hideProgress()
                }
                onIo(dispatchers) {
                    delay(5000)
                }
                onUi(dispatchers) {
                    doOnResult()
                }
            }.onFailure {
                onUi(dispatchers) {
                    hideProgress()
                    view.showText(it.message)
                }
            }
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
