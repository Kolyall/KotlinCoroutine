package com.github.kotlincoroutine.usecases

import com.github.kotlincoroutine.base.UseCase
import com.github.kotlincoroutine.usecases.models.Place
import com.github.kotlincoroutine.usecases.models.User

class LoadPlaceUseCase : UseCase<List<Place>>() {

    /*list flatMap to another list example*/
    override suspend fun doOnBackground(): List<Place> {
        return getListUser().asyncMap {
            getPlaceByUserId(it.id)
        }
    }

    private fun getPlaceByUserId(id: String): Place {
        return Place("placeId_$id")
    }

    private fun getListUser(): List<User> {
        return mutableListOf(
            User("userId1"),
            User("userId2"),
            User("userId3"),
            User("userId4")
        )
    }

}


