package com.kotlincalendar.client.data.repository

import com.kotlincalendar.client.domain.model.AuthState
import com.kotlincalendar.client.fake.FakeAuthRemoteDataSource
import com.kotlincalendar.client.fake.FakeTokenStorage
import com.kotlincalendar.proto.auth.v1.AuthTokens
import com.kotlincalendar.proto.auth.v1.LoginResponse
import com.kotlincalendar.proto.auth.v1.LogoutResponse
import com.kotlincalendar.proto.auth.v1.RefreshTokenResponse
import com.kotlincalendar.proto.auth.v1.SignUpResponse
import com.kotlincalendar.proto.auth.v1.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import com.kotlincalendar.client.domain.model.AuthTokens as DomainTokens
import com.kotlincalendar.client.domain.model.User as DomainUser

class AuthRepositoryImplTest {
    private val protoUser =
        User(
            id = "user-1",
            email = "test@example.com",
            display_name = "Test User",
            created_at =
                com.squareup.wire.Instant
                    .ofEpochMilli(1_700_000_000_000L),
        )

    private val protoTokens =
        AuthTokens(
            access_token = "access-123",
            access_token_expires_at =
                com.squareup.wire.Instant
                    .ofEpochMilli(1_700_000_900_000L),
            refresh_token = "refresh-456",
            refresh_token_expires_at =
                com.squareup.wire.Instant
                    .ofEpochMilli(1_700_086_400_000L),
        )

    private val domainUser =
        DomainUser(
            id = "user-1",
            email = "test@example.com",
            displayName = "Test User",
            createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        )

    private val domainTokens =
        DomainTokens(
            accessToken = "access-123",
            accessTokenExpiresAt = Instant.fromEpochMilliseconds(1_700_000_900_000L),
            refreshToken = "refresh-456",
            refreshTokenExpiresAt = Instant.fromEpochMilliseconds(1_700_086_400_000L),
        )

    @Test
    fun signUp_success_savesTokensAndUpdatesAuthState() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val storage = FakeTokenStorage()
            val repository = AuthRepositoryImpl(remote, storage)

            remote.signUpResult =
                Result.success(
                    SignUpResponse(
                        user = protoUser,
                        tokens = protoTokens,
                    ),
                )

            val result = repository.signUp("test@example.com", "password", "Test User")

            assertTrue(result.isSuccess)
            assertEquals(domainUser, result.getOrNull())
            assertEquals(domainTokens, storage.tokens)
            assertEquals(AuthState.Authenticated(domainUser), repository.authState.value)
            assertEquals(domainUser, repository.getSavedUser())
        }

    @Test
    fun signUp_failure_leavesStateUnauthenticated() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val storage = FakeTokenStorage()
            val repository = AuthRepositoryImpl(remote, storage)

            remote.signUpResult = Result.failure(RuntimeException("Sign up failed"))

            val result = repository.signUp("test@example.com", "password", "Test User")

            assertTrue(result.isFailure)
            assertNull(storage.tokens)
            assertEquals(AuthState.Unauthenticated, repository.authState.value)
            assertNull(repository.getSavedUser())
        }

    @Test
    fun login_success_savesTokensAndUpdatesAuthState() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val storage = FakeTokenStorage()
            val repository = AuthRepositoryImpl(remote, storage)

            remote.loginResult =
                Result.success(
                    LoginResponse(
                        user = protoUser,
                        tokens = protoTokens,
                    ),
                )

            val result = repository.login("test@example.com", "password")

            assertTrue(result.isSuccess)
            assertEquals(domainUser, result.getOrNull())
            assertEquals(domainTokens, storage.tokens)
            assertEquals(AuthState.Authenticated(domainUser), repository.authState.value)
            assertEquals(domainUser, repository.getSavedUser())
        }

    @Test
    fun login_failure_leavesStateUnauthenticated() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val storage = FakeTokenStorage()
            val repository = AuthRepositoryImpl(remote, storage)

            remote.loginResult = Result.failure(RuntimeException("Invalid credentials"))

            val result = repository.login("test@example.com", "wrong-password")

            assertTrue(result.isFailure)
            assertNull(storage.tokens)
            assertEquals(AuthState.Unauthenticated, repository.authState.value)
        }

    @Test
    fun refreshToken_success_savesNewTokens() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val storage = FakeTokenStorage(initialTokens = domainTokens)
            val repository = AuthRepositoryImpl(remote, storage)

            val newProtoTokens =
                AuthTokens(
                    access_token = "access-new",
                    access_token_expires_at =
                        com.squareup.wire.Instant
                            .ofEpochMilli(1_700_001_800_000L),
                    refresh_token = "refresh-new",
                    refresh_token_expires_at =
                        com.squareup.wire.Instant
                            .ofEpochMilli(1_700_090_000_000L),
                )
            val newDomainTokens =
                DomainTokens(
                    accessToken = "access-new",
                    accessTokenExpiresAt = Instant.fromEpochMilliseconds(1_700_001_800_000L),
                    refreshToken = "refresh-new",
                    refreshTokenExpiresAt = Instant.fromEpochMilliseconds(1_700_090_000_000L),
                )

            remote.refreshTokenResult =
                Result.success(
                    RefreshTokenResponse(tokens = newProtoTokens),
                )

            val result = repository.refreshToken()

            assertTrue(result.isSuccess)
            assertEquals(newDomainTokens, result.getOrNull())
            assertEquals(newDomainTokens, storage.tokens)
        }

    @Test
    fun refreshToken_failsWhenNoTokensInStorage() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val storage = FakeTokenStorage()
            val repository = AuthRepositoryImpl(remote, storage)

            val result = repository.refreshToken()

            assertTrue(result.isFailure)
        }

    @Test
    fun logout_clearsTokensAndResetsAuthState() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val storage = FakeTokenStorage(initialTokens = domainTokens)
            val repository = AuthRepositoryImpl(remote, storage)

            remote.loginResult =
                Result.success(
                    LoginResponse(user = protoUser, tokens = protoTokens),
                )
            repository.login("test@example.com", "password")

            remote.logoutResult = Result.success(LogoutResponse())

            val result = repository.logout()

            assertTrue(result.isSuccess)
            assertEquals("refresh-456", remote.lastLogoutRequest?.refresh_token)
            assertEquals("access-123", remote.lastLogoutAccessToken)
            assertNull(storage.tokens)
            assertEquals(AuthState.Unauthenticated, repository.authState.value)
        }

    @Test
    fun logout_clearsTokensEvenIfRemoteFails() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val storage = FakeTokenStorage(initialTokens = domainTokens)
            val repository = AuthRepositoryImpl(remote, storage)

            remote.loginResult =
                Result.success(
                    LoginResponse(user = protoUser, tokens = protoTokens),
                )
            repository.login("test@example.com", "password")

            remote.logoutResult = Result.failure(RuntimeException("Network error"))

            val result = repository.logout()

            assertTrue(result.isSuccess)
            assertNull(storage.tokens)
            assertEquals(AuthState.Unauthenticated, repository.authState.value)
        }
}
