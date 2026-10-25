package com.kotlincalendar.server.user.repository

interface UserRepository {
    suspend fun findById(id: String): UserRecord?

    suspend fun findByEmail(email: String): UserRecord?

    suspend fun existsByEmail(email: String): Boolean

    suspend fun save(user: UserRecord): UserRecord
}
