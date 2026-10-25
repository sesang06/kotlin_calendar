package com.kotlincalendar.client.data.remote

import com.kotlincalendar.client.data.remote.http.HttpAuthRemoteDataSource
import com.kotlincalendar.proto.auth.v1.AuthTokens
import com.kotlincalendar.proto.auth.v1.LoginRequest
import com.kotlincalendar.proto.auth.v1.LoginResponse
import com.kotlincalendar.proto.auth.v1.LogoutRequest
import com.kotlincalendar.proto.auth.v1.LogoutResponse
import com.kotlincalendar.proto.auth.v1.RefreshTokenRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenResponse
import com.kotlincalendar.proto.auth.v1.SignUpRequest
import com.kotlincalendar.proto.auth.v1.SignUpResponse
import com.kotlincalendar.proto.auth.v1.User
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpAuthRemoteDataSourceTest {
    private val sampleUser =
        User(
            id = "user-1",
            email = "user@example.com",
            display_name = "User One",
            created_at_epoch_millis = 1_700_000_000_000L,
        )

    private val sampleTokens =
        AuthTokens(
            access_token = "access-token-123",
            access_token_expires_at_epoch_millis = 1_700_000_900_000L,
            refresh_token = "refresh-token-456",
            refresh_token_expires_at_epoch_millis = 1_700_086_400_000L,
        )

    @Test
    fun signUp_encodesRequestAndDecodesResponse() =
        runTest {
            val request =
                SignUpRequest(
                    email = "user@example.com",
                    password = "password123",
                    display_name = "User One",
                )
            val expectedResponse =
                SignUpResponse(
                    user = sampleUser,
                    tokens = sampleTokens,
                )

            val engine =
                MockEngine { httpRequest ->
                    assertEquals(HttpMethod.Post, httpRequest.method)
                    assertEquals("/api/auth/signup", httpRequest.url.encodedPath)
                    val decodedRequest = SignUpRequest.ADAPTER.decode(httpRequest.body.toByteArray())
                    assertEquals(request, decodedRequest)

                    respond(
                        content = expectedResponse.encode(),
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/x-protobuf"),
                    )
                }

            val source = HttpAuthRemoteDataSource(HttpClient(engine))
            val response = source.signUp(request)

            assertEquals(expectedResponse, response)
        }

    @Test
    fun login_encodesRequestAndDecodesResponse() =
        runTest {
            val request =
                LoginRequest(
                    email = "user@example.com",
                    password = "password123",
                )
            val expectedResponse =
                LoginResponse(
                    user = sampleUser,
                    tokens = sampleTokens,
                )

            val engine =
                MockEngine { httpRequest ->
                    assertEquals(HttpMethod.Post, httpRequest.method)
                    assertEquals("/api/auth/login", httpRequest.url.encodedPath)
                    val decodedRequest = LoginRequest.ADAPTER.decode(httpRequest.body.toByteArray())
                    assertEquals(request, decodedRequest)

                    respond(
                        content = expectedResponse.encode(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/x-protobuf"),
                    )
                }

            val source = HttpAuthRemoteDataSource(HttpClient(engine))
            val response = source.login(request)

            assertEquals(expectedResponse, response)
        }

    @Test
    fun refreshToken_encodesRequestAndDecodesResponse() =
        runTest {
            val request =
                RefreshTokenRequest(
                    refresh_token = "refresh-token-456",
                )
            val expectedResponse =
                RefreshTokenResponse(
                    tokens = sampleTokens,
                )

            val engine =
                MockEngine { httpRequest ->
                    assertEquals(HttpMethod.Post, httpRequest.method)
                    assertEquals("/api/auth/refresh", httpRequest.url.encodedPath)
                    val decodedRequest = RefreshTokenRequest.ADAPTER.decode(httpRequest.body.toByteArray())
                    assertEquals(request, decodedRequest)

                    respond(
                        content = expectedResponse.encode(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/x-protobuf"),
                    )
                }

            val source = HttpAuthRemoteDataSource(HttpClient(engine))
            val response = source.refreshToken(request)

            assertEquals(expectedResponse, response)
        }

    @Test
    fun logout_sendsBearerAuthHeaderAndDecodesResponse() =
        runTest {
            val request =
                LogoutRequest(
                    refresh_token = "refresh-token-456",
                )

            val engine =
                MockEngine { httpRequest ->
                    assertEquals(HttpMethod.Post, httpRequest.method)
                    assertEquals("/api/auth/logout", httpRequest.url.encodedPath)
                    assertEquals("Bearer access-token-123", httpRequest.headers[HttpHeaders.Authorization])
                    val decodedRequest = LogoutRequest.ADAPTER.decode(httpRequest.body.toByteArray())
                    assertEquals(request, decodedRequest)

                    respond(
                        content = ByteArray(0),
                        status = HttpStatusCode.NoContent,
                        headers = headersOf(HttpHeaders.ContentType, "application/x-protobuf"),
                    )
                }

            val source = HttpAuthRemoteDataSource(HttpClient(engine))
            val response = source.logout(request, "access-token-123")

            assertEquals(LogoutResponse(), response)
        }
}
