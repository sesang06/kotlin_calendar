package com.kotlincalendar.client.domain.repository

import com.kotlincalendar.client.domain.model.User

interface UserStorage {
    suspend fun getUser(): User?

    suspend fun saveUser(user: User)

    suspend fun clearUser()
}
