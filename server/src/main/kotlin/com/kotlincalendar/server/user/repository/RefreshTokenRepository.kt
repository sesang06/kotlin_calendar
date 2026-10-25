package com.kotlincalendar.server.user.repository

interface RefreshTokenRepository {
    suspend fun findByToken(token: String): RefreshTokenRecord?

    suspend fun save(record: RefreshTokenRecord): RefreshTokenRecord

    /** 아직 폐기되지 않은 토큰만 [revokedAt]으로 폐기한다. */
    suspend fun revoke(
        token: String,
        revokedAt: Long,
    ): Boolean

    suspend fun revokeAllByUserId(
        userId: String,
        revokedAt: Long,
    ): Int
}
