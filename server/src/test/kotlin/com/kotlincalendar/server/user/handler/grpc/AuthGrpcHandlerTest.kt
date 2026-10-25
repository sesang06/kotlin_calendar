package com.kotlincalendar.server.user.handler.grpc

import com.kotlincalendar.proto.auth.v1.AuthApiGrpcKt
import com.kotlincalendar.proto.auth.v1.LoginRequest
import com.kotlincalendar.proto.auth.v1.LogoutRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenRequest
import com.kotlincalendar.proto.auth.v1.SignUpRequest
import com.kotlincalendar.server.common.error.ConflictException
import com.kotlincalendar.server.common.error.UnauthenticatedException
import com.kotlincalendar.server.common.grpc.AUTH_USER_ID_KEY
import com.kotlincalendar.server.user.domain.AuthResult
import com.kotlincalendar.server.user.domain.AuthTokens
import com.kotlincalendar.server.user.domain.User
import com.kotlincalendar.server.user.service.UserService
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class AuthGrpcHandlerTest {
    private val userService = mockk<UserService>()
    private val fixedNow = Instant.parse("2026-10-25T12:00:00Z")

    private lateinit var serverName: String
    private lateinit var server: io.grpc.Server
    private lateinit var channel: io.grpc.ManagedChannel
    private lateinit var stub: AuthApiGrpcKt.AuthApiCoroutineStub

    // Context interceptor for testing authenticated RPCs
    private var testUserId: String? = null

    private val authTestInterceptor =
        object : ServerInterceptor {
            override fun <ReqT, RespT> interceptCall(
                call: ServerCall<ReqT, RespT>,
                headers: Metadata,
                next: ServerCallHandler<ReqT, RespT>,
            ): ServerCall.Listener<ReqT> {
                val context =
                    if (testUserId != null) {
                        Context.current().withValue(AUTH_USER_ID_KEY, testUserId)
                    } else {
                        Context.current()
                    }
                return Contexts.interceptCall(context, call, headers, next)
            }
        }

    @BeforeEach
    fun setUp() {
        serverName = InProcessServerBuilder.generateName()
        server =
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(AuthGrpcHandler(userService), authTestInterceptor))
                .build()
                .start()

        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
        stub = AuthApiGrpcKt.AuthApiCoroutineStub(channel)
    }

    @AfterEach
    fun tearDown() {
        channel.shutdownNow()
        server.shutdownNow()
    }

    @Test
    fun `signUp 성공 시 SignUpResponse를 반환한다`() =
        runTest {
            val user =
                User(
                    id = "u-1",
                    email = "test@example.com",
                    displayName = "Tester",
                    createdAt = fixedNow,
                )
            val tokens =
                AuthTokens(
                    accessToken = "acc-token",
                    accessTokenExpiresAtEpochMillis = 1000L,
                    refreshToken = "ref-token",
                    refreshTokenExpiresAtEpochMillis = 2000L,
                )
            coEvery { userService.signUp(any()) } returns AuthResult(user, tokens)

            val request =
                SignUpRequest
                    .newBuilder()
                    .setEmail("test@example.com")
                    .setPassword("password123")
                    .setDisplayName("Tester")
                    .build()

            val response = stub.signUp(request)

            assertEquals("u-1", response.user.id)
            assertEquals("test@example.com", response.user.email)
            assertEquals("acc-token", response.tokens.accessToken)
        }

    @Test
    fun `signUp 시 ConflictException 발생 시 ALREADY_EXISTS gRPC 상태로 변환된다`() =
        runTest {
            coEvery { userService.signUp(any()) } throws ConflictException("already exists")

            val request =
                SignUpRequest
                    .newBuilder()
                    .setEmail("test@example.com")
                    .setPassword("password123")
                    .setDisplayName("Tester")
                    .build()

            val exception =
                assertFailsWith<StatusException> {
                    stub.signUp(request)
                }

            assertEquals(Status.Code.ALREADY_EXISTS, exception.status.code)
        }

    @Test
    fun `login 성공 시 LoginResponse를 반환한다`() =
        runTest {
            val user =
                User(
                    id = "u-1",
                    email = "test@example.com",
                    displayName = "Tester",
                    createdAt = fixedNow,
                )
            val tokens =
                AuthTokens(
                    accessToken = "acc-token",
                    accessTokenExpiresAtEpochMillis = 1000L,
                    refreshToken = "ref-token",
                    refreshTokenExpiresAtEpochMillis = 2000L,
                )
            coEvery { userService.login(any()) } returns AuthResult(user, tokens)

            val request =
                LoginRequest
                    .newBuilder()
                    .setEmail("test@example.com")
                    .setPassword("password123")
                    .build()

            val response = stub.login(request)

            assertEquals("u-1", response.user.id)
            assertEquals("acc-token", response.tokens.accessToken)
        }

    @Test
    fun `login 실패 시 UNAUTHENTICATED gRPC 상태로 변환된다`() =
        runTest {
            coEvery { userService.login(any()) } throws UnauthenticatedException("invalid credentials")

            val request =
                LoginRequest
                    .newBuilder()
                    .setEmail("test@example.com")
                    .setPassword("wrong")
                    .build()

            val exception =
                assertFailsWith<StatusException> {
                    stub.login(request)
                }

            assertEquals(Status.Code.UNAUTHENTICATED, exception.status.code)
        }

    @Test
    fun `refreshToken 성공 시 RefreshTokenResponse를 반환한다`() =
        runTest {
            val tokens =
                AuthTokens(
                    accessToken = "new-acc",
                    accessTokenExpiresAtEpochMillis = 3000L,
                    refreshToken = "new-ref",
                    refreshTokenExpiresAtEpochMillis = 4000L,
                )
            coEvery { userService.refreshToken(any()) } returns tokens

            val request =
                RefreshTokenRequest
                    .newBuilder()
                    .setRefreshToken("old-ref")
                    .build()

            val response = stub.refreshToken(request)

            assertEquals("new-acc", response.tokens.accessToken)
            assertEquals("new-ref", response.tokens.refreshToken)
        }

    @Test
    fun `인증된 사용자의 logout 성공 시 LogoutResponse를 반환한다`() =
        runTest {
            testUserId = "user-123"
            coEvery { userService.logout(any()) } returns Unit

            val request =
                LogoutRequest
                    .newBuilder()
                    .setRefreshToken("ref-token-1")
                    .build()

            stub.logout(request)

            coVerify(exactly = 1) { userService.logout(any()) }
        }

    @Test
    fun `인증되지 않은 사용자의 logout 호출 시 UNAUTHENTICATED gRPC 상태를 반환한다`() =
        runTest {
            testUserId = null

            val request =
                LogoutRequest
                    .newBuilder()
                    .setRefreshToken("ref-token-1")
                    .build()

            val exception =
                assertFailsWith<StatusException> {
                    stub.logout(request)
                }

            assertEquals(Status.Code.UNAUTHENTICATED, exception.status.code)
            coVerify(exactly = 0) { userService.logout(any()) }
        }
}
