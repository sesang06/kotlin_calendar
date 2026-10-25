package com.kotlincalendar.client.data.mapper

import com.kotlincalendar.client.domain.model.AuthTokens
import com.kotlincalendar.client.domain.model.User
import kotlin.time.Instant
import com.kotlincalendar.proto.auth.v1.AuthTokens as ProtoAuthTokens
import com.kotlincalendar.proto.auth.v1.User as ProtoUser

fun ProtoUser.toDomain(): User =
    User(
        id = id,
        email = email,
        displayName = display_name,
        createdAt = Instant.fromEpochMilliseconds(created_at_epoch_millis),
    )

fun ProtoAuthTokens.toDomain(): AuthTokens =
    AuthTokens(
        accessToken = access_token,
        accessTokenExpiresAt = Instant.fromEpochMilliseconds(access_token_expires_at_epoch_millis),
        refreshToken = refresh_token,
        refreshTokenExpiresAt = Instant.fromEpochMilliseconds(refresh_token_expires_at_epoch_millis),
    )

fun User.toProto(): ProtoUser =
    ProtoUser(
        id = id,
        email = email,
        display_name = displayName,
        created_at_epoch_millis = createdAt.toEpochMilliseconds(),
    )

fun AuthTokens.toProto(): ProtoAuthTokens =
    ProtoAuthTokens(
        access_token = accessToken,
        access_token_expires_at_epoch_millis = accessTokenExpiresAt.toEpochMilliseconds(),
        refresh_token = refreshToken,
        refresh_token_expires_at_epoch_millis = refreshTokenExpiresAt.toEpochMilliseconds(),
    )
