package com.github.kotlincoroutine

interface MainFragmentView : HasProgress {
    fun showText(result: String?)
}
