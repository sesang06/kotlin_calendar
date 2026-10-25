package com.kotlincalendar.server.user.repository

import com.kotlincalendar.server.common.database.epochMillisToUtcOffsetDateTime
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.security.MessageDigest

class ExposedRefreshTokenRepository(
    private val database: Database,
) : RefreshTokenRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO, database) { block() }

    override suspend fun findByToken(token: String): RefreshTokenRecord? =
        dbQuery {
            RefreshTokenTable
                .selectAll()
                .where { RefreshTokenTable.tokenHash eq sha256Hex(token) }
                .singleOrNull()
                ?.toRefreshTokenRecord(token)
        }

    override suspend fun save(record: RefreshTokenRecord): RefreshTokenRecord =
        dbQuery {
            RefreshTokenTable.insert {
                it[id] = record.id
                it[userId] = record.userId
                it[tokenHash] = sha256Hex(record.token)
                it[expiresAt] = record.expiresAt.epochMillisToUtcOffsetDateTime()
                it[createdAt] = record.createdAt.epochMillisToUtcOffsetDateTime()
                it[revokedAt] = record.revokedAt?.epochMillisToUtcOffsetDateTime()
            }
            record
        }

    override suspend fun revoke(
        token: String,
        revokedAt: Long,
    ): Boolean =
        dbQuery {
            RefreshTokenTable.update({
                (RefreshTokenTable.tokenHash eq sha256Hex(token)) and RefreshTokenTable.revokedAt.isNull()
            }) {
                it[RefreshTokenTable.revokedAt] = revokedAt.epochMillisToUtcOffsetDateTime()
            } > 0
        }

    override suspend fun revokeAllByUserId(
        userId: String,
        revokedAt: Long,
    ): Int =
        dbQuery {
            RefreshTokenTable.update({
                (RefreshTokenTable.userId eq userId) and RefreshTokenTable.revokedAt.isNull()
            }) {
                it[RefreshTokenTable.revokedAt] = revokedAt.epochMillisToUtcOffsetDateTime()
            }
        }

    private fun sha256Hex(token: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
