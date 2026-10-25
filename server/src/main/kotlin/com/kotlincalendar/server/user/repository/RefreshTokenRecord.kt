package com.kotlincalendar.server.user.repository

/** [token]은 원문. 저장소에는 해시만 남는다 ([RefreshTokenTable]). */
data class RefreshTokenRecord(
    val id: String,
    val token: String,
    val userId: String,
    val expiresAt: Long,
    val createdAt: Long,
    val revokedAt: Long? = null,
) {
    val revoked: Boolean get() = revokedAt != null
}
