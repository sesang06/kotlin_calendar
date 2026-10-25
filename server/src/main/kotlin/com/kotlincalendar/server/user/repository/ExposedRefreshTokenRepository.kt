package com.kotlincalendar.server.user.repository

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class ExposedRefreshTokenRepository(
    private val database: Database,
) : RefreshTokenRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO, database) { block() }

    override suspend fun findByToken(token: String): RefreshTokenRecord? =
        dbQuery {
            RefreshTokenTable
                .selectAll()
                .where { RefreshTokenTable.token eq token }
                .singleOrNull()
                ?.toRefreshTokenRecord()
        }

    override suspend fun save(record: RefreshTokenRecord): RefreshTokenRecord =
        dbQuery {
            RefreshTokenTable.insert {
                it[token] = record.token
                it[userId] = record.userId
                it[expiresAt] = record.expiresAt
                it[createdAt] = record.createdAt
                it[revoked] = record.revoked
            }
            record
        }

    override suspend fun revoke(token: String): Boolean =
        dbQuery {
            RefreshTokenTable.update({ RefreshTokenTable.token eq token }) {
                it[revoked] = true
            } > 0
        }

    override suspend fun revokeAllByUserId(userId: String): Int =
        dbQuery {
            RefreshTokenTable.update({ RefreshTokenTable.userId eq userId }) {
                it[revoked] = true
            }
        }
}
