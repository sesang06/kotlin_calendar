package com.kotlincalendar.client.fake

import com.kotlincalendar.client.domain.model.User
import com.kotlincalendar.client.domain.repository.UserStorage

class FakeUserStorage(
    initialUser: User? = null,
) : UserStorage {
    var user: User? = initialUser

    override suspend fun getUser(): User? = user

    override suspend fun saveUser(user: User) {
        this.user = user
    }

    override suspend fun clearUser() {
        user = null
    }
}
