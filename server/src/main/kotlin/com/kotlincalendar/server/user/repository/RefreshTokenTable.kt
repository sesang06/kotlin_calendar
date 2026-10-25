package com.kotlincalendar.server.user.repository

import com.kotlincalendar.server.common.database.toEpochMillis
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

/** 스키마는 `db/migration/postgresql/V2__create_refresh_tokens.sql`. 원문 토큰 대신 SHA-256 hex만 저장한다. */
object RefreshTokenTable : Table("refresh_tokens") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val tokenHash = char("token_hash", 64)
    val expiresAt = timestampWithTimeZone("expires_at")
    val createdAt = timestampWithTimeZone("created_at")
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** 해시만 저장하므로 원문 [token]은 조회에 쓴 값을 그대로 돌려준다. */
fun ResultRow.toRefreshTokenRecord(token: String): RefreshTokenRecord =
    RefreshTokenRecord(
        id = this[RefreshTokenTable.id],
        token = token,
        userId = this[RefreshTokenTable.userId],
        expiresAt = this[RefreshTokenTable.expiresAt].toEpochMillis(),
        createdAt = this[RefreshTokenTable.createdAt].toEpochMillis(),
        revokedAt = this[RefreshTokenTable.revokedAt]?.toEpochMillis(),
    )
