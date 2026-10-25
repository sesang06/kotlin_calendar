package com.kotlincalendar.client.data.remote.auth

import com.kotlincalendar.client.data.mapper.toWireInstant
import com.kotlincalendar.client.data.repository.AuthRepositoryImpl
import com.kotlincalendar.client.domain.model.AuthState
import com.kotlincalendar.client.domain.model.AuthTokens
import com.kotlincalendar.client.fake.FakeAuthRemoteDataSource
import com.kotlincalendar.client.fake.FakeTokenStorage
import com.kotlincalendar.proto.auth.v1.LoginResponse
import com.kotlincalendar.proto.auth.v1.RefreshTokenResponse
import com.kotlincalendar.proto.auth.v1.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant
import com.kotlincalendar.proto.auth.v1.AuthTokens as ProtoTokens

class TokenAuthSessionTest {
    private val expiresAt = Instant.fromEpochMilliseconds(1_700_000_900_000L)

    private fun protoTokens(access: String) =
        ProtoTokens(
            access_token = access,
            access_token_expires_at = expiresAt.toWireInstant(),
            refresh_token = "refresh-$access",
            refresh_token_expires_at = expiresAt.toWireInstant(),
        )

    private val remote = FakeAuthRemoteDataSource()
    private val storage = FakeTokenStorage()
    private val repository = AuthRepositoryImpl(remote, storage)
    private val session = TokenAuthSession(storage, repository)

    private suspend fun loggedIn() {
        remote.loginResult =
            Result.success(
                LoginResponse(
                    user = User(id = "user-1", email = "a@b.c", created_at = expiresAt.toWireInstant()),
                    tokens = protoTokens("access-1"),
                ),
            )
        repository.login("a@b.c", "pw").getOrThrow()
    }

    @Test
    fun accessToken_readsFromStorage() =
        runTest {
            assertNull(session.accessToken())

            loggedIn()

            assertEquals("access-1", session.accessToken())
        }

    @Test
    fun refresh_success_returnsNewTokenAndStaysAuthenticated() =
        runTest {
            loggedIn()
            remote.refreshTokenResult = Result.success(RefreshTokenResponse(tokens = protoTokens("access-2")))

            val token = session.refreshAccessToken(rejectedToken = "access-1")

            assertEquals("access-2", token)
            assertEquals("refresh-access-1", remote.lastRefreshTokenRequest?.refresh_token)
            assertEquals("access-2", storage.tokens?.accessToken)
            assertIs<AuthState.Authenticated>(repository.authState.value)
        }

    @Test
    fun refresh_failure_throwsAndLogsOut() =
        runTest {
            loggedIn()
            remote.refreshTokenResult = Result.failure(IllegalStateException("invalid refresh token"))

            assertFailsWith<IllegalStateException> { session.refreshAccessToken(rejectedToken = "access-1") }

            assertNull(storage.tokens)
            assertEquals(AuthState.Unauthenticated, repository.authState.value)
        }

    @Test
    fun refresh_skipsNetworkWhenTokenAlreadyRotated() =
        runTest {
            storage.tokens =
                AuthTokens(
                    accessToken = "access-2",
                    accessTokenExpiresAt = expiresAt,
                    refreshToken = "refresh-2",
                    refreshTokenExpiresAt = expiresAt,
                )

            val token = session.refreshAccessToken(rejectedToken = "access-1")

            assertEquals("access-2", token)
            assertNull(remote.lastRefreshTokenRequest)
        }
}
