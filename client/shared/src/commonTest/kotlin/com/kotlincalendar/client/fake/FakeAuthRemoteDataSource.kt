package com.kotlincalendar.client.fake

import com.kotlincalendar.client.data.remote.AuthRemoteDataSource
import com.kotlincalendar.proto.auth.v1.LoginRequest
import com.kotlincalendar.proto.auth.v1.LoginResponse
import com.kotlincalendar.proto.auth.v1.LogoutRequest
import com.kotlincalendar.proto.auth.v1.LogoutResponse
import com.kotlincalendar.proto.auth.v1.RefreshTokenRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenResponse
import com.kotlincalendar.proto.auth.v1.SignUpRequest
import com.kotlincalendar.proto.auth.v1.SignUpResponse
import com.kotlincalendar.proto.auth.v1.User as ProtoUser

class FakeAuthRemoteDataSource : AuthRemoteDataSource {
    var signUpResult: Result<SignUpResponse> = Result.failure(NotImplementedError())
    var loginResult: Result<LoginResponse> = Result.failure(NotImplementedError())
    var refreshTokenResult: Result<RefreshTokenResponse> = Result.failure(NotImplementedError())
    var logoutResult: Result<LogoutResponse> = Result.success(LogoutResponse())

    var lastSignUpRequest: SignUpRequest? = null
    var lastLoginRequest: LoginRequest? = null
    var lastRefreshTokenRequest: RefreshTokenRequest? = null
    var lastLogoutRequest: LogoutRequest? = null
    var lastLogoutAccessToken: String? = null

    override suspend fun signUp(request: SignUpRequest): SignUpResponse {
        lastSignUpRequest = request
        return signUpResult.getOrThrow()
    }

    override suspend fun login(request: LoginRequest): LoginResponse {
        lastLoginRequest = request
        return loginResult.getOrThrow()
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse {
        lastRefreshTokenRequest = request
        return refreshTokenResult.getOrThrow()
    }

    override suspend fun logout(
        request: LogoutRequest,
        accessToken: String,
    ): LogoutResponse {
        lastLogoutRequest = request
        lastLogoutAccessToken = accessToken
        return logoutResult.getOrThrow()
    }

    var getMeResults: MutableList<Result<ProtoUser>> = mutableListOf()
    val getMeAccessTokens = mutableListOf<String>()

    override suspend fun getMe(accessToken: String): ProtoUser {
        getMeAccessTokens += accessToken
        return getMeResults.removeAt(0).getOrThrow()
    }
}
