package com.kotlincalendar.server.user.repository

import com.kotlincalendar.server.user.domain.User
import kotlin.time.Instant

data class UserRecord(
    val id: String,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val createdAtEpochMillis: Long,
) {
    fun toDomain(): User =
        User(
            id = id,
            email = email,
            displayName = displayName,
            createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
        )
}
