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

    /** 서버에서 내 정보를 다시 받아 저장한다. access token이 만료됐으면 한 번 갱신 후 재시도한다. */
    suspend fun refreshCurrentUser(): Result<User>

    /** 저장된 세션을 읽어 refresh token이 유효하면 로그인 상태로 복원한다. 복원된 사용자, 없으면 null. */
    suspend fun restoreSession(): User?
}
