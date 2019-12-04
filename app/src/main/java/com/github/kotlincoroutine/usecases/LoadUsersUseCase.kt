package com.github.kotlincoroutine.usecases

import com.github.kotlincoroutine.base.UseCase
import com.github.kotlincoroutine.usecases.models.User
import kotlinx.coroutines.delay

class LoadUsersUseCase : UseCase<List<User>>() {

    override suspend fun doOnBackground(): List<User> {
        delay(2000)
        val list = mutableListOf<User>()
        for(i in 1..100){
            list.add(User("Name $i"))
        }
        return list
    }

}