package com.kotlincalendar.server.user.repository

interface RefreshTokenRepository {
    suspend fun findByToken(token: String): RefreshTokenRecord?

    suspend fun save(record: RefreshTokenRecord): RefreshTokenRecord

    suspend fun revoke(token: String): Boolean

    suspend fun revokeAllByUserId(userId: String): Int
}
