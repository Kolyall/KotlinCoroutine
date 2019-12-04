package com.github.kotlincoroutine.base

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext

abstract class BaseFragmentScope : CoroutineScope, LifecycleObserver {

    private lateinit var supervisorJob: Job

    override val coroutineContext: CoroutineContext
        get() = supervisorJob + Dispatchers.IO

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        supervisorJob = SupervisorJob()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onDestroy() {
        cancelAllJobs()
    }

    private fun cancelAllJobs() {
//        job.cancel()
        coroutineContext.cancelChildren()
    }
}