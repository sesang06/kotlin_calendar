package com.kotlincalendar.client.data.repository

import com.kotlincalendar.client.data.mapper.toWireInstant
import com.kotlincalendar.client.domain.model.AuthState
import com.kotlincalendar.client.fake.FakeAuthRemoteDataSource
import com.kotlincalendar.client.fake.FakeTokenStorage
import com.kotlincalendar.client.fake.FakeUserStorage
import com.kotlincalendar.client.fake.FixedClock
import com.kotlincalendar.proto.auth.v1.AuthTokens
import com.kotlincalendar.proto.auth.v1.LoginResponse
import com.kotlincalendar.proto.auth.v1.LogoutResponse
import com.kotlincalendar.proto.auth.v1.RefreshTokenResponse
import com.kotlincalendar.proto.auth.v1.SignUpResponse
import com.kotlincalendar.proto.auth.v1.User
import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
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
                Instant
                    .fromEpochMilliseconds(1_700_000_000_000L)
                    .toWireInstant(),
        )

    private val protoTokens =
        AuthTokens(
            access_token = "access-123",
            access_token_expires_at =
                Instant
                    .fromEpochMilliseconds(1_700_000_900_000L)
                    .toWireInstant(),
            refresh_token = "refresh-456",
            refresh_token_expires_at =
                Instant
                    .fromEpochMilliseconds(1_700_086_400_000L)
                    .toWireInstant(),
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
            val repository = AuthRepositoryImpl(remote, storage, FakeUserStorage())

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
            val repository = AuthRepositoryImpl(remote, storage, FakeUserStorage())

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
            val repository = AuthRepositoryImpl(remote, storage, FakeUserStorage())

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
            val repository = AuthRepositoryImpl(remote, storage, FakeUserStorage())

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
            val repository = AuthRepositoryImpl(remote, storage, FakeUserStorage())

            val newProtoTokens =
                AuthTokens(
                    access_token = "access-new",
                    access_token_expires_at =
                        Instant
                            .fromEpochMilliseconds(1_700_001_800_000L)
                            .toWireInstant(),
                    refresh_token = "refresh-new",
                    refresh_token_expires_at =
                        Instant
                            .fromEpochMilliseconds(1_700_090_000_000L)
                            .toWireInstant(),
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
            val repository = AuthRepositoryImpl(remote, storage, FakeUserStorage())

            val result = repository.refreshToken()

            assertTrue(result.isFailure)
        }

    @Test
    fun refreshToken_remoteFailure_clearsTokensAndLogsOut() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val storage = FakeTokenStorage()
            val repository = AuthRepositoryImpl(remote, storage, FakeUserStorage())
            remote.loginResult = Result.success(LoginResponse(user = protoUser, tokens = protoTokens))
            repository.login("test@example.com", "password")

            remote.refreshTokenResult = Result.failure(RuntimeException("invalid refresh token"))

            val result = repository.refreshToken()

            assertTrue(result.isFailure)
            assertNull(storage.tokens)
            assertEquals(AuthState.Unauthenticated, repository.authState.value)
            assertNull(repository.getSavedUser())
        }

    @Test
    fun logout_clearsTokensAndResetsAuthState() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val storage = FakeTokenStorage(initialTokens = domainTokens)
            val repository = AuthRepositoryImpl(remote, storage, FakeUserStorage())

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
            val repository = AuthRepositoryImpl(remote, storage, FakeUserStorage())

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

    @Test
    fun login_success_savesUser() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val userStorage = FakeUserStorage()
            val repository = AuthRepositoryImpl(remote, FakeTokenStorage(), userStorage)
            remote.loginResult = Result.success(LoginResponse(user = protoUser, tokens = protoTokens))

            repository.login("test@example.com", "password")

            assertEquals(domainUser, userStorage.user)
        }

    @Test
    fun restoreSession_validRefreshToken_restoresAuthenticatedState() =
        runTest {
            val repository =
                AuthRepositoryImpl(
                    FakeAuthRemoteDataSource(),
                    FakeTokenStorage(initialTokens = domainTokens),
                    FakeUserStorage(initialUser = domainUser),
                    clock = FixedClock(domainTokens.refreshTokenExpiresAt - 1.seconds),
                )

            val restored = repository.restoreSession()

            assertEquals(domainUser, restored)
            assertEquals(AuthState.Authenticated(domainUser), repository.authState.value)
        }

    @Test
    fun restoreSession_expiredRefreshToken_clearsSession() =
        runTest {
            val tokenStorage = FakeTokenStorage(initialTokens = domainTokens)
            val userStorage = FakeUserStorage(initialUser = domainUser)
            val repository =
                AuthRepositoryImpl(
                    FakeAuthRemoteDataSource(),
                    tokenStorage,
                    userStorage,
                    clock = FixedClock(domainTokens.refreshTokenExpiresAt),
                )

            val restored = repository.restoreSession()

            assertNull(restored)
            assertNull(tokenStorage.tokens)
            assertNull(userStorage.user)
            assertEquals(AuthState.Unauthenticated, repository.authState.value)
        }

    @Test
    fun restoreSession_missingUser_clearsTokens() =
        runTest {
            val tokenStorage = FakeTokenStorage(initialTokens = domainTokens)
            val repository =
                AuthRepositoryImpl(
                    FakeAuthRemoteDataSource(),
                    tokenStorage,
                    FakeUserStorage(),
                    clock = FixedClock(domainTokens.refreshTokenExpiresAt - 1.seconds),
                )

            assertNull(repository.restoreSession())
            assertNull(tokenStorage.tokens)
        }

    @Test
    fun logout_clearsSavedUser() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val userStorage = FakeUserStorage(initialUser = domainUser)
            val repository = AuthRepositoryImpl(remote, FakeTokenStorage(initialTokens = domainTokens), userStorage)
            remote.logoutResult = Result.success(LogoutResponse())

            repository.logout()

            assertNull(userStorage.user)
        }

    @Test
    fun refreshCurrentUser_success_savesUserAndAuthenticates() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val userStorage = FakeUserStorage()
            val repository = AuthRepositoryImpl(remote, FakeTokenStorage(initialTokens = domainTokens), userStorage)
            remote.getMeResults += Result.success(protoUser)

            val result = repository.refreshCurrentUser()

            assertEquals(domainUser, result.getOrThrow())
            assertEquals(listOf("access-123"), remote.getMeAccessTokens)
            assertEquals(domainUser, userStorage.user)
            assertEquals(AuthState.Authenticated(domainUser), repository.authState.value)
        }

    @Test
    fun refreshCurrentUser_unauthenticated_refreshesTokenAndRetriesOnce() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val repository = AuthRepositoryImpl(remote, FakeTokenStorage(initialTokens = domainTokens), FakeUserStorage())
            remote.getMeResults += Result.failure(GrpcException(GrpcStatus.UNAUTHENTICATED, "expired"))
            remote.getMeResults += Result.success(protoUser)
            remote.refreshTokenResult =
                Result.success(RefreshTokenResponse(tokens = protoTokens.copy(access_token = "access-new")))

            val result = repository.refreshCurrentUser()

            assertTrue(result.isSuccess)
            assertEquals(listOf("access-123", "access-new"), remote.getMeAccessTokens)
        }

    @Test
    fun refreshCurrentUser_withoutTokens_fails() =
        runTest {
            val remote = FakeAuthRemoteDataSource()
            val repository = AuthRepositoryImpl(remote, FakeTokenStorage(), FakeUserStorage())

            assertTrue(repository.refreshCurrentUser().isFailure)
            assertTrue(remote.getMeAccessTokens.isEmpty())
        }
}
