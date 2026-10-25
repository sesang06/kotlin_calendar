package com.kotlincalendar.server.user.domain

data class AuthTokens(
    val accessToken: String,
    val accessTokenExpiresAtEpochMillis: Long,
    val refreshToken: String,
    val refreshTokenExpiresAtEpochMillis: Long,
)
