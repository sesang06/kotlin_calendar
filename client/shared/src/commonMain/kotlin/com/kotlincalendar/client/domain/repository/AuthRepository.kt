package com.kotlincalendar.client.domain.repository

import com.kotlincalendar.client.domain.model.AuthState
import com.kotlincalendar.client.domain.model.AuthTokens
import com.kotlincalendar.client.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<User>

    suspend fun login(
        email: String,
        password: String,
    ): Result<User>

    suspend fun refreshToken(): Result<AuthTokens>

    suspend fun logout(): Result<Unit>

    suspend fun getSavedUser(): User?
}
