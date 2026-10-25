package com.kotlincalendar.client.data.repository

import com.kotlincalendar.client.common.runCatchingCancellable
import com.kotlincalendar.client.data.mapper.toDomain
import com.kotlincalendar.client.data.remote.AuthRemoteDataSource
import com.kotlincalendar.client.data.remote.auth.isUnauthenticated
import com.kotlincalendar.client.domain.model.AuthState
import com.kotlincalendar.client.domain.model.AuthTokens
import com.kotlincalendar.client.domain.model.User
import com.kotlincalendar.client.domain.repository.AuthRepository
import com.kotlincalendar.client.domain.repository.TokenStorage
import com.kotlincalendar.client.domain.repository.UserStorage
import com.kotlincalendar.proto.auth.v1.LoginRequest
import com.kotlincalendar.proto.auth.v1.LogoutRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenRequest
import com.kotlincalendar.proto.auth.v1.SignUpRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val tokenStorage: TokenStorage,
    private val userStorage: UserStorage,
    private val clock: Clock = Clock.System,
) : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<User> =
        runCatchingCancellable {
            val response =
                remoteDataSource.signUp(
                    SignUpRequest(
                        email = email,
                        password = password,
                        display_name = displayName,
                    ),
                )
            val tokens = response.tokens?.toDomain() ?: error("Missing tokens in SignUpResponse")
            val user = response.user?.toDomain() ?: error("Missing user in SignUpResponse")
            tokenStorage.saveTokens(tokens)
            userStorage.saveUser(user)
            _authState.value = AuthState.Authenticated(user)
            user
        }

    override suspend fun login(
        email: String,
        password: String,
    ): Result<User> =
        runCatchingCancellable {
            val response =
                remoteDataSource.login(
                    LoginRequest(
                        email = email,
                        password = password,
                    ),
                )
            val tokens = response.tokens?.toDomain() ?: error("Missing tokens in LoginResponse")
            val user = response.user?.toDomain() ?: error("Missing user in LoginResponse")
            tokenStorage.saveTokens(tokens)
            userStorage.saveUser(user)
            _authState.value = AuthState.Authenticated(user)
            user
        }

    override suspend fun refreshToken(): Result<AuthTokens> =
        runCatchingCancellable {
            val currentTokens = tokenStorage.getTokens() ?: error("No refresh token available")
            val response =
                remoteDataSource.refreshToken(
                    RefreshTokenRequest(refresh_token = currentTokens.refreshToken),
                )
            val newTokens = response.tokens?.toDomain() ?: error("Missing tokens in RefreshTokenResponse")
            tokenStorage.saveTokens(newTokens)
            newTokens
        }.onFailure {
            // refresh token으로도 세션을 이어갈 수 없으면 로그아웃 상태로 되돌린다.
            clearSession()
        }

    override suspend fun logout(): Result<Unit> =
        runCatchingCancellable {
            val currentTokens = tokenStorage.getTokens()
            if (currentTokens != null) {
                try {
                    remoteDataSource.logout(
                        LogoutRequest(refresh_token = currentTokens.refreshToken),
                        accessToken = currentTokens.accessToken,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    // Even if network logout fails, clear local tokens
                }
            }
            clearSession()
        }

    override suspend fun getSavedUser(): User? =
        when (val state = _authState.value) {
            is AuthState.Authenticated -> state.user
            AuthState.Unauthenticated -> null
        }

    override suspend fun refreshCurrentUser(): Result<User> =
        runCatchingCancellable {
            val tokens = tokenStorage.getTokens() ?: error("Not logged in")
            val proto =
                try {
                    remoteDataSource.getMe(tokens.accessToken)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    if (!e.isUnauthenticated()) throw e
                    remoteDataSource.getMe(refreshToken().getOrThrow().accessToken)
                }
            val user = proto.toDomain()
            userStorage.saveUser(user)
            _authState.value = AuthState.Authenticated(user)
            user
        }

    override suspend fun restoreSession(): User? {
        val tokens = tokenStorage.getTokens()
        val user = userStorage.getUser()
        if (tokens == null || user == null || tokens.refreshTokenExpiresAt <= clock.now()) {
            clearSession()
            return null
        }
        _authState.value = AuthState.Authenticated(user)
        return user
    }

    private suspend fun clearSession() {
        tokenStorage.clearTokens()
        userStorage.clearUser()
        _authState.value = AuthState.Unauthenticated
    }
}
