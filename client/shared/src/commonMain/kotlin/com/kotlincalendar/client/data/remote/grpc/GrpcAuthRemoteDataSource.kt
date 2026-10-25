package com.kotlincalendar.client.data.remote.grpc

import com.kotlincalendar.client.data.remote.AuthRemoteDataSource
import com.kotlincalendar.proto.auth.v1.AuthApiClient
import com.kotlincalendar.proto.auth.v1.GetMeRequest
import com.kotlincalendar.proto.auth.v1.LoginRequest
import com.kotlincalendar.proto.auth.v1.LoginResponse
import com.kotlincalendar.proto.auth.v1.LogoutRequest
import com.kotlincalendar.proto.auth.v1.LogoutResponse
import com.kotlincalendar.proto.auth.v1.RefreshTokenRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenResponse
import com.kotlincalendar.proto.auth.v1.SignUpRequest
import com.kotlincalendar.proto.auth.v1.SignUpResponse
import com.kotlincalendar.proto.auth.v1.User as ProtoUser

class GrpcAuthRemoteDataSource(
    private val api: AuthApiClient,
) : AuthRemoteDataSource {
    override suspend fun signUp(request: SignUpRequest): SignUpResponse = api.SignUp().execute(request)

    override suspend fun login(request: LoginRequest): LoginResponse = api.Login().execute(request)

    override suspend fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse = api.RefreshToken().execute(request)

    override suspend fun logout(
        request: LogoutRequest,
        accessToken: String,
    ): LogoutResponse = api.Logout().withBearer(accessToken).execute(request)

    override suspend fun getMe(accessToken: String): ProtoUser = api.GetMe().withBearer(accessToken).execute(GetMeRequest())
}
