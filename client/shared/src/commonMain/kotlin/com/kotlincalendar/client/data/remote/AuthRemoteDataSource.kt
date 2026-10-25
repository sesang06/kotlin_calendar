package com.kotlincalendar.client.data.remote

import com.kotlincalendar.proto.auth.v1.LoginRequest
import com.kotlincalendar.proto.auth.v1.LoginResponse
import com.kotlincalendar.proto.auth.v1.LogoutRequest
import com.kotlincalendar.proto.auth.v1.LogoutResponse
import com.kotlincalendar.proto.auth.v1.RefreshTokenRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenResponse
import com.kotlincalendar.proto.auth.v1.SignUpRequest
import com.kotlincalendar.proto.auth.v1.SignUpResponse
import com.kotlincalendar.proto.auth.v1.User as ProtoUser

interface AuthRemoteDataSource {
    suspend fun signUp(request: SignUpRequest): SignUpResponse

    suspend fun login(request: LoginRequest): LoginResponse

    suspend fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse

    suspend fun logout(
        request: LogoutRequest,
        accessToken: String,
    ): LogoutResponse

    suspend fun getMe(accessToken: String): ProtoUser
}
