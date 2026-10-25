package com.kotlincalendar.server.user.repository

import com.kotlincalendar.server.common.database.epochMillisToUtcOffsetDateTime
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ExposedUserRepository(
    private val database: Database,
) : UserRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO, database) { block() }

    override suspend fun findById(id: String): UserRecord? =
        dbQuery {
            UserTable
                .selectAll()
                .where { UserTable.id eq id }
                .singleOrNull()
                ?.toUserRecord()
        }

    override suspend fun findByEmail(email: String): UserRecord? =
        dbQuery {
            UserTable
                .selectAll()
                .where { UserTable.email eq email }
                .singleOrNull()
                ?.toUserRecord()
        }

    override suspend fun existsByEmail(email: String): Boolean =
        dbQuery {
            UserTable
                .selectAll()
                .where { UserTable.email eq email }
                .limit(1)
                .any()
        }

    override suspend fun save(user: UserRecord): UserRecord =
        dbQuery {
            UserTable.insert {
                it[id] = user.id
                it[email] = user.email
                it[displayName] = user.displayName
                it[passwordHash] = user.passwordHash
                it[createdAt] = user.createdAtEpochMillis.epochMillisToUtcOffsetDateTime()
                it[updatedAt] = user.createdAtEpochMillis.epochMillisToUtcOffsetDateTime()
            }
            user
        }
}
