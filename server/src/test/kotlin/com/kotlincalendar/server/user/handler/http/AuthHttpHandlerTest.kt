package com.kotlincalendar.server.user.handler.http

import com.kotlincalendar.proto.auth.v1.LoginRequest
import com.kotlincalendar.proto.auth.v1.LoginResponse
import com.kotlincalendar.proto.auth.v1.LogoutRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenResponse
import com.kotlincalendar.proto.auth.v1.SignUpRequest
import com.kotlincalendar.proto.auth.v1.SignUpResponse
import com.kotlincalendar.proto.calendar.v1.ErrorResponse
import com.kotlincalendar.server.common.auth.TokenProvider
import com.kotlincalendar.server.common.error.ConflictException
import com.kotlincalendar.server.common.error.UnauthenticatedException
import com.kotlincalendar.server.common.http.ProtobufContentType
import com.kotlincalendar.server.plugins.configureKoin
import com.kotlincalendar.server.plugins.configureRouting
import com.kotlincalendar.server.plugins.configureStatusPages
import com.kotlincalendar.server.user.domain.AuthResult
import com.kotlincalendar.server.user.domain.AuthTokens
import com.kotlincalendar.server.user.domain.User
import com.kotlincalendar.server.user.service.UserService
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import com.kotlincalendar.proto.auth.v1.User as UserMessage

class AuthHttpHandlerTest {
    private val userService = mockk<UserService>()
    private val tokenProvider = mockk<TokenProvider>()

    private val fixedNow = Instant.parse("2026-10-25T12:00:00Z")

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            environment { config = MapApplicationConfig() }
            application {
                configureKoin(
                    listOf(
                        module {
                            single { userService }
                            single { tokenProvider }
                            single { AuthHttpHandler(get(), get()) }
                        },
                    ),
                )
                configureStatusPages()
                configureRouting()
            }
            block()
        }

    @Test
    fun `POST signup 성공 시 201과 SignUpResponse를 protobuf로 반환한다`() =
        withApp {
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
                    accessTokenExpiresAt = fixedNow + 15.minutes,
                    refreshToken = "ref-token",
                    refreshTokenExpiresAt = fixedNow + 30.days,
                )
            coEvery { userService.signUp(any()) } returns AuthResult(user, tokens)

            val requestBody =
                SignUpRequest
                    .newBuilder()
                    .setEmail("test@example.com")
                    .setPassword("password123")
                    .setDisplayName("Tester")
                    .build()

            val response =
                client.post("/api/auth/signup") {
                    contentType(ProtobufContentType)
                    setBody(requestBody.toByteArray())
                }

            assertEquals(HttpStatusCode.Created, response.status)
            val responseProto = SignUpResponse.parseFrom(response.body<ByteArray>())
            assertEquals("u-1", responseProto.user.id)
            assertEquals("test@example.com", responseProto.user.email)
            assertEquals("acc-token", responseProto.tokens.accessToken)
        }

    @Test
    fun `POST signup 시 이미 존재하는 이메일이면 409와 ErrorResponse를 반환한다`() =
        withApp {
            coEvery { userService.signUp(any()) } throws ConflictException("email already exists")

            val requestBody =
                SignUpRequest
                    .newBuilder()
                    .setEmail("test@example.com")
                    .setPassword("password123")
                    .setDisplayName("Tester")
                    .build()

            val response =
                client.post("/api/auth/signup") {
                    contentType(ProtobufContentType)
                    setBody(requestBody.toByteArray())
                }

            assertEquals(HttpStatusCode.Conflict, response.status)
            val error = ErrorResponse.parseFrom(response.body<ByteArray>())
            assertEquals("email already exists", error.message)
        }

    @Test
    fun `POST login 성공 시 200과 LoginResponse를 반환한다`() =
        withApp {
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
                    accessTokenExpiresAt = fixedNow + 15.minutes,
                    refreshToken = "ref-token",
                    refreshTokenExpiresAt = fixedNow + 30.days,
                )
            coEvery { userService.login(any()) } returns AuthResult(user, tokens)

            val requestBody =
                LoginRequest
                    .newBuilder()
                    .setEmail("test@example.com")
                    .setPassword("password123")
                    .build()

            val response =
                client.post("/api/auth/login") {
                    contentType(ProtobufContentType)
                    setBody(requestBody.toByteArray())
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseProto = LoginResponse.parseFrom(response.body<ByteArray>())
            assertEquals("u-1", responseProto.user.id)
            assertEquals("acc-token", responseProto.tokens.accessToken)
        }

    @Test
    fun `POST login 실패 시 401과 ErrorResponse를 반환한다`() =
        withApp {
            coEvery { userService.login(any()) } throws UnauthenticatedException("invalid email or password")

            val requestBody =
                LoginRequest
                    .newBuilder()
                    .setEmail("test@example.com")
                    .setPassword("wrong")
                    .build()

            val response =
                client.post("/api/auth/login") {
                    contentType(ProtobufContentType)
                    setBody(requestBody.toByteArray())
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            val error = ErrorResponse.parseFrom(response.body<ByteArray>())
            assertEquals("invalid email or password", error.message)
        }

    @Test
    fun `POST refresh 성공 시 200과 RefreshTokenResponse를 반환한다`() =
        withApp {
            val tokens =
                AuthTokens(
                    accessToken = "new-acc",
                    accessTokenExpiresAt = fixedNow + 15.minutes,
                    refreshToken = "new-ref",
                    refreshTokenExpiresAt = fixedNow + 30.days,
                )
            coEvery { userService.refreshToken(any()) } returns tokens

            val requestBody =
                RefreshTokenRequest
                    .newBuilder()
                    .setRefreshToken("old-ref")
                    .build()

            val response =
                client.post("/api/auth/refresh") {
                    contentType(ProtobufContentType)
                    setBody(requestBody.toByteArray())
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseProto = RefreshTokenResponse.parseFrom(response.body<ByteArray>())
            assertEquals("new-acc", responseProto.tokens.accessToken)
            assertEquals("new-ref", responseProto.tokens.refreshToken)
        }

    @Test
    fun `POST logout 성공 시 204 NoContent를 반환한다`() =
        withApp {
            every { tokenProvider.verifyAccessToken("valid-access-token", any()) } returns "u-1"
            coEvery { userService.logout(any()) } returns Unit

            val requestBody =
                LogoutRequest
                    .newBuilder()
                    .setRefreshToken("ref-to-revoke")
                    .build()

            val response =
                client.post("/api/auth/logout") {
                    header(HttpHeaders.Authorization, "Bearer valid-access-token")
                    contentType(ProtobufContentType)
                    setBody(requestBody.toByteArray())
                }

            assertEquals(HttpStatusCode.NoContent, response.status)
            coVerify(exactly = 1) { userService.logout(any()) }
        }

    @Test
    fun `POST logout 시 Authorization 헤더가 없으면 401을 반환한다`() =
        withApp {
            val requestBody =
                LogoutRequest
                    .newBuilder()
                    .setRefreshToken("ref-to-revoke")
                    .build()

            val response =
                client.post("/api/auth/logout") {
                    contentType(ProtobufContentType)
                    setBody(requestBody.toByteArray())
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            coVerify(exactly = 0) { userService.logout(any()) }
        }

    @Test
    fun `GET me는 200과 사용자 정보를 protobuf로 반환한다`() =
        withApp {
            every { tokenProvider.verifyAccessToken("valid-token", any()) } returns "u-1"
            coEvery { userService.getMe("u-1") } returns
                User(id = "u-1", email = "test@example.com", displayName = "Tester", createdAt = fixedNow)

            val response =
                client.get("/api/auth/me") {
                    header(HttpHeaders.Authorization, "Bearer valid-token")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val user = UserMessage.parseFrom(response.body<ByteArray>())
            assertEquals("u-1", user.id)
            assertEquals("Tester", user.displayName)
        }

    @Test
    fun `GET me에 인증 헤더가 없으면 401이고 Service를 호출하지 않는다`() =
        withApp {
            val response = client.get("/api/auth/me")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            coVerify(exactly = 0) { userService.getMe(any()) }
        }
}
