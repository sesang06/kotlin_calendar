package com.kotlincalendar.server.user.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

object RefreshTokenTable : Table("refresh_tokens") {
    val token = varchar("token", 128)
    val userId = varchar("user_id", 36)
    val expiresAt = long("expires_at")
    val createdAt = long("created_at")
    val revoked = bool("revoked").default(false)

    override val primaryKey = PrimaryKey(token)

    init {
        index(isUnique = false, userId)
    }
}

fun ResultRow.toRefreshTokenRecord(): RefreshTokenRecord =
    RefreshTokenRecord(
        token = this[RefreshTokenTable.token],
        userId = this[RefreshTokenTable.userId],
        expiresAt = this[RefreshTokenTable.expiresAt],
        createdAt = this[RefreshTokenTable.createdAt],
        revoked = this[RefreshTokenTable.revoked],
    )
