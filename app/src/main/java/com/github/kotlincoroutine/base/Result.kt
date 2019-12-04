package com.github.kotlincoroutine.base

data class Result<out T>(val data: T? = null, val error: Throwable? = null) {
    fun withResult(doOnSuccess: (T) -> Unit = {}, doOnError: (Throwable) -> Unit = {}) {
        data?.let(doOnSuccess) ?: error?.let(doOnError)
    }
}