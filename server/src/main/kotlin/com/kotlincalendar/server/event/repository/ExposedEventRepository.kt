package com.kotlincalendar.server.event.repository

import com.kotlincalendar.server.common.database.toUtcOffsetDateTime
import com.kotlincalendar.server.event.domain.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import kotlin.time.Instant

class ExposedEventRepository(
    private val database: Database,
) : EventRepository {
    private suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T = newSuspendedTransaction(Dispatchers.IO, database) { block() }

    override suspend fun findById(id: String): Event? =
        dbQuery {
            EventTable
                .selectAll()
                .where { EventTable.id eq id }
                .singleOrNull()
                ?.toEvent()
        }

    override suspend fun findByIdAndOwnerUserId(
        id: String,
        ownerUserId: String,
    ): Event? =
        dbQuery {
            EventTable
                .selectAll()
                .where { (EventTable.id eq id) and (EventTable.ownerUserId eq ownerUserId) }
                .singleOrNull()
                ?.toEvent()
        }

    override suspend fun findByDate(
        ownerUserId: String,
        date: LocalDate,
    ): List<Event> {
        val dayStart = date.atStartOfDayIn(TimeZone.UTC)
        val dayEnd = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.UTC)
        return findOverlapping(ownerUserId, dayStart, dayEnd)
    }

    override suspend fun findOverlapping(
        ownerUserId: String,
        start: Instant,
        end: Instant,
    ): List<Event> =
        dbQuery {
            EventTable
                .selectAll()
                .where {
                    (EventTable.ownerUserId eq ownerUserId) and
                        (EventTable.startAt less end.toUtcOffsetDateTime()) and
                        (EventTable.endAt greater start.toUtcOffsetDateTime())
                }.orderBy(EventTable.startAt to SortOrder.ASC)
                .map { it.toEvent() }
        }

    override suspend fun existsOverlapping(
        ownerUserId: String,
        start: Instant,
        end: Instant,
    ): Boolean =
        dbQuery {
            EventTable
                .selectAll()
                .where {
                    (EventTable.ownerUserId eq ownerUserId) and
                        (EventTable.startAt less end.toUtcOffsetDateTime()) and
                        (EventTable.endAt greater start.toUtcOffsetDateTime())
                }.limit(1)
                .any()
        }

    override suspend fun save(event: Event): Event =
        dbQuery {
            val exists = EventTable.selectAll().where { EventTable.id eq event.id }.any()
            if (exists) {
                EventTable.update({ EventTable.id eq event.id }) {
                    it[title] = event.title
                    it[startAt] = event.start.toUtcOffsetDateTime()
                    it[endAt] = event.end.toUtcOffsetDateTime()
                    it[createdAt] = event.createdAt.toUtcOffsetDateTime()
                    it[description] = event.description
                    it[ownerUserId] = event.ownerUserId
                }
            } else {
                EventTable.insert {
                    it[id] = event.id
                    it[title] = event.title
                    it[startAt] = event.start.toUtcOffsetDateTime()
                    it[endAt] = event.end.toUtcOffsetDateTime()
                    it[createdAt] = event.createdAt.toUtcOffsetDateTime()
                    it[description] = event.description
                    it[ownerUserId] = event.ownerUserId
                }
            }
            event
        }

    override suspend fun deleteById(id: String): Boolean =
        dbQuery {
            EventTable.deleteWhere { EventTable.id eq id } > 0
        }

    override suspend fun deleteByIdAndOwnerUserId(
        id: String,
        ownerUserId: String,
    ): Boolean =
        dbQuery {
            EventTable.deleteWhere { (EventTable.id eq id) and (EventTable.ownerUserId eq ownerUserId) } > 0
        }
}
