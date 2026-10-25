package com.kotlincalendar.client.domain.model

import kotlin.time.Instant

data class AuthTokens(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
)
