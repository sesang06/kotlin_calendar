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
        createdAt = created_at?.toKotlinInstant() ?: Instant.DISTANT_PAST,
    )

fun ProtoAuthTokens.toDomain(): AuthTokens =
    AuthTokens(
        accessToken = access_token,
        accessTokenExpiresAt = access_token_expires_at?.toKotlinInstant() ?: Instant.DISTANT_PAST,
        refreshToken = refresh_token,
        refreshTokenExpiresAt = refresh_token_expires_at?.toKotlinInstant() ?: Instant.DISTANT_PAST,
    )

fun User.toProto(): ProtoUser =
    ProtoUser(
        id = id,
        email = email,
        display_name = displayName,
        created_at = createdAt.toWireInstant(),
    )

fun AuthTokens.toProto(): ProtoAuthTokens =
    ProtoAuthTokens(
        access_token = accessToken,
        access_token_expires_at = accessTokenExpiresAt.toWireInstant(),
        refresh_token = refreshToken,
        refresh_token_expires_at = refreshTokenExpiresAt.toWireInstant(),
    )
