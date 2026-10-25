package com.kotlincalendar.server.user.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

object UserTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 320).uniqueIndex("idx_users_email")
    val displayName = varchar("display_name", 100)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

fun ResultRow.toUserRecord(): UserRecord =
    UserRecord(
        id = this[UserTable.id],
        email = this[UserTable.email],
        displayName = this[UserTable.displayName],
        passwordHash = this[UserTable.passwordHash],
        createdAtEpochMillis = this[UserTable.createdAt],
    )
