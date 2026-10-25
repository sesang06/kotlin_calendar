package com.kotlincalendar.server.user.handler.grpc

import com.kotlincalendar.proto.auth.v1.AuthApiGrpcKt
import com.kotlincalendar.proto.auth.v1.GetMeRequest
import com.kotlincalendar.proto.auth.v1.LoginRequest
import com.kotlincalendar.proto.auth.v1.LoginResponse
import com.kotlincalendar.proto.auth.v1.LogoutRequest
import com.kotlincalendar.proto.auth.v1.LogoutResponse
import com.kotlincalendar.proto.auth.v1.RefreshTokenRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenResponse
import com.kotlincalendar.proto.auth.v1.SignUpRequest
import com.kotlincalendar.proto.auth.v1.SignUpResponse
import com.kotlincalendar.server.common.error.UnauthenticatedException
import com.kotlincalendar.server.common.grpc.AUTH_USER_ID_KEY
import com.kotlincalendar.server.common.grpc.grpcCall
import com.kotlincalendar.server.user.handler.toCommand
import com.kotlincalendar.server.user.handler.toLoginResponse
import com.kotlincalendar.server.user.handler.toProto
import com.kotlincalendar.server.user.handler.toRefreshTokenResponse
import com.kotlincalendar.server.user.handler.toSignUpResponse
import com.kotlincalendar.server.user.service.UserService
import com.kotlincalendar.proto.auth.v1.User as UserMessage

class AuthGrpcHandler(
    private val userService: UserService,
) : AuthApiGrpcKt.AuthApiCoroutineImplBase() {
    override suspend fun signUp(request: SignUpRequest): SignUpResponse =
        grpcCall {
            userService.signUp(request.toCommand()).toSignUpResponse()
        }

    override suspend fun login(request: LoginRequest): LoginResponse =
        grpcCall {
            userService.login(request.toCommand()).toLoginResponse()
        }

    override suspend fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse =
        grpcCall {
            userService.refreshToken(request.toCommand()).toRefreshTokenResponse()
        }

    override suspend fun logout(request: LogoutRequest): LogoutResponse =
        grpcCall {
            val userId = AUTH_USER_ID_KEY.get() ?: throw UnauthenticatedException("unauthenticated")
            userService.logout(request.toCommand(userId))
            LogoutResponse.getDefaultInstance()
        }

    override suspend fun getMe(request: GetMeRequest): UserMessage =
        grpcCall {
            val userId = AUTH_USER_ID_KEY.get() ?: throw UnauthenticatedException("unauthenticated")
            userService.getMe(userId).toProto()
        }
}
