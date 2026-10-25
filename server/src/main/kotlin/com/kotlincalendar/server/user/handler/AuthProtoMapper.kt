package com.kotlincalendar.server.user.handler

import com.kotlincalendar.proto.auth.v1.LoginRequest
import com.kotlincalendar.proto.auth.v1.LoginResponse
import com.kotlincalendar.proto.auth.v1.LogoutRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenResponse
import com.kotlincalendar.proto.auth.v1.SignUpRequest
import com.kotlincalendar.proto.auth.v1.SignUpResponse
import com.kotlincalendar.server.user.domain.AuthResult
import com.kotlincalendar.server.user.domain.AuthTokens
import com.kotlincalendar.server.user.domain.LoginCommand
import com.kotlincalendar.server.user.domain.LogoutCommand
import com.kotlincalendar.server.user.domain.RefreshTokenCommand
import com.kotlincalendar.server.user.domain.SignUpCommand
import com.kotlincalendar.server.user.domain.User
import com.kotlincalendar.proto.auth.v1.AuthTokens as AuthTokensMessage
import com.kotlincalendar.proto.auth.v1.User as UserMessage

fun User.toProto(): UserMessage =
    UserMessage
        .newBuilder()
        .setId(id)
        .setEmail(email)
        .setDisplayName(displayName)
        .setCreatedAtEpochMillis(createdAt.toEpochMilliseconds())
        .build()

fun AuthTokens.toProto(): AuthTokensMessage =
    AuthTokensMessage
        .newBuilder()
        .setAccessToken(accessToken)
        .setAccessTokenExpiresAtEpochMillis(accessTokenExpiresAtEpochMillis)
        .setRefreshToken(refreshToken)
        .setRefreshTokenExpiresAtEpochMillis(refreshTokenExpiresAtEpochMillis)
        .build()

fun AuthResult.toSignUpResponse(): SignUpResponse =
    SignUpResponse
        .newBuilder()
        .setUser(user.toProto())
        .setTokens(tokens.toProto())
        .build()

fun AuthResult.toLoginResponse(): LoginResponse =
    LoginResponse
        .newBuilder()
        .setUser(user.toProto())
        .setTokens(tokens.toProto())
        .build()

fun AuthTokens.toRefreshTokenResponse(): RefreshTokenResponse =
    RefreshTokenResponse
        .newBuilder()
        .setTokens(toProto())
        .build()

fun SignUpRequest.toCommand(): SignUpCommand =
    SignUpCommand(
        email = email,
        password = password,
        displayName = displayName,
    )

fun LoginRequest.toCommand(): LoginCommand =
    LoginCommand(
        email = email,
        password = password,
    )

fun RefreshTokenRequest.toCommand(): RefreshTokenCommand =
    RefreshTokenCommand(
        refreshToken = refreshToken,
    )

fun LogoutRequest.toCommand(userId: String): LogoutCommand =
    LogoutCommand(
        refreshToken = refreshToken,
        userId = userId,
    )
