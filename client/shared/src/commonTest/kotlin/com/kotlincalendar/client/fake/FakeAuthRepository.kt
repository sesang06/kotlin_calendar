package com.kotlincalendar.client.fake

import com.kotlincalendar.client.domain.model.AuthState
import com.kotlincalendar.client.domain.model.AuthTokens
import com.kotlincalendar.client.domain.model.User
import com.kotlincalendar.client.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository(
    initialState: AuthState = AuthState.Unauthenticated,
) : AuthRepository {
    private val _authState = MutableStateFlow(initialState)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var signUpResult: Result<User> = Result.failure(NotImplementedError())
    var loginResult: Result<User> = Result.failure(NotImplementedError())
    var refreshTokenResult: Result<AuthTokens> = Result.failure(NotImplementedError())
    var logoutResult: Result<Unit> = Result.success(Unit)
    var restoredUser: User? = null
    var refreshCurrentUserResult: Result<User> = Result.failure(NotImplementedError())

    var lastSignUpEmail: String? = null
    var lastSignUpPassword: String? = null
    var lastSignUpDisplayName: String? = null

    var lastLoginEmail: String? = null
    var lastLoginPassword: String? = null

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<User> {
        lastSignUpEmail = email
        lastSignUpPassword = password
        lastSignUpDisplayName = displayName
        return signUpResult.also {
            it.getOrNull()?.let { user -> _authState.value = AuthState.Authenticated(user) }
        }
    }

    override suspend fun login(
        email: String,
        password: String,
    ): Result<User> {
        lastLoginEmail = email
        lastLoginPassword = password
        return loginResult.also {
            it.getOrNull()?.let { user -> _authState.value = AuthState.Authenticated(user) }
        }
    }

    override suspend fun refreshToken(): Result<AuthTokens> = refreshTokenResult

    override suspend fun logout(): Result<Unit> =
        logoutResult.also {
            if (it.isSuccess) {
                _authState.value = AuthState.Unauthenticated
            }
        }

    override suspend fun getSavedUser(): User? =
        when (val state = _authState.value) {
            is AuthState.Authenticated -> state.user
            AuthState.Unauthenticated -> null
        }

    override suspend fun restoreSession(): User? = restoredUser?.also { _authState.value = AuthState.Authenticated(it) }

    override suspend fun refreshCurrentUser(): Result<User> =
        refreshCurrentUserResult.also {
            it.getOrNull()?.let { user -> _authState.value = AuthState.Authenticated(user) }
        }
}
