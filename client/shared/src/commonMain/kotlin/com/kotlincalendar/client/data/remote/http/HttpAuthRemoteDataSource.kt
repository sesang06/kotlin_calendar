package com.kotlincalendar.client.data.remote.http

import com.kotlincalendar.client.data.remote.AuthRemoteDataSource
import com.kotlincalendar.proto.auth.v1.LoginRequest
import com.kotlincalendar.proto.auth.v1.LoginResponse
import com.kotlincalendar.proto.auth.v1.LogoutRequest
import com.kotlincalendar.proto.auth.v1.LogoutResponse
import com.kotlincalendar.proto.auth.v1.RefreshTokenRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenResponse
import com.kotlincalendar.proto.auth.v1.SignUpRequest
import com.kotlincalendar.proto.auth.v1.SignUpResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import com.kotlincalendar.proto.auth.v1.User as ProtoUser

class HttpAuthRemoteDataSource(
    private val client: HttpClient,
) : AuthRemoteDataSource {
    private val protobufContentType = ContentType("application", "x-protobuf")

    override suspend fun signUp(request: SignUpRequest): SignUpResponse {
        val bytes =
            client
                .post("/api/auth/signup") {
                    contentType(protobufContentType)
                    setBody(request.encode())
                }.body<ByteArray>()
        return SignUpResponse.ADAPTER.decode(bytes)
    }

    override suspend fun login(request: LoginRequest): LoginResponse {
        val bytes =
            client
                .post("/api/auth/login") {
                    contentType(protobufContentType)
                    setBody(request.encode())
                }.body<ByteArray>()
        return LoginResponse.ADAPTER.decode(bytes)
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse {
        val bytes =
            client
                .post("/api/auth/refresh") {
                    contentType(protobufContentType)
                    setBody(request.encode())
                }.body<ByteArray>()
        return RefreshTokenResponse.ADAPTER.decode(bytes)
    }

    override suspend fun logout(
        request: LogoutRequest,
        accessToken: String,
    ): LogoutResponse {
        val bytes =
            client
                .post("/api/auth/logout") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(protobufContentType)
                    setBody(request.encode())
                }.body<ByteArray>()
        return if (bytes.isEmpty()) LogoutResponse() else LogoutResponse.ADAPTER.decode(bytes)
    }

    override suspend fun getMe(accessToken: String): ProtoUser {
        val bytes =
            client
                .get("/api/auth/me") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }.body<ByteArray>()
        return ProtoUser.ADAPTER.decode(bytes)
    }
}
