package com.kotlincalendar.server.user.repository

data class RefreshTokenRecord(
    val token: String,
    val userId: String,
    val expiresAt: Long,
    val createdAt: Long,
    val revoked: Boolean = false,
)
