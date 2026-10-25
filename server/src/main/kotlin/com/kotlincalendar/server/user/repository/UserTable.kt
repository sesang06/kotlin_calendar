package com.kotlincalendar.server.user.repository

import com.kotlincalendar.server.common.database.toEpochMillis
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

/** 스키마는 `db/migration/postgresql/V1__create_users.sql`. */
object UserTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 320)
    val passwordHash = varchar("password_hash", 255)
    val displayName = varchar("display_name", 100)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

fun ResultRow.toUserRecord(): UserRecord =
    UserRecord(
        id = this[UserTable.id],
        email = this[UserTable.email],
        displayName = this[UserTable.displayName],
        passwordHash = this[UserTable.passwordHash],
        createdAtEpochMillis = this[UserTable.createdAt].toEpochMillis(),
    )
