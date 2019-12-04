package com.github.kotlincoroutine.api

import com.github.kotlincoroutine.base.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class ApiService {
    suspend fun loadTitleData(): Result<String> {
        delay(2000)
        return try {
            val invoke = "TitleData"
//            throw IllegalArgumentException("Any error")
            Result(invoke)
        } catch (exception: Exception) {
            Result(null, exception)
        }
    }

    suspend fun doWork() = coroutineScope {
        async {
            delay(1000)
            //            throw IllegalArgumentException("Any error")
            return@async "doWork"
        }.await()
    }

}