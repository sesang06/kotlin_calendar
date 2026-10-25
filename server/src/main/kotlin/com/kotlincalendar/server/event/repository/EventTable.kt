package com.kotlincalendar.server.event.repository

import com.kotlincalendar.server.common.database.toKotlinInstant
import com.kotlincalendar.server.event.domain.Event
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

/** 스키마는 `db/migration/postgresql/V3__create_events.sql`. */
object EventTable : Table("events") {
    val id = varchar("id", 36)
    val ownerUserId = varchar("owner_user_id", 36)
    val title = varchar("title", 200)
    val description = varchar("description", 2000).default("")
    val startAt = timestampWithTimeZone("start_at")
    val endAt = timestampWithTimeZone("end_at")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

fun ResultRow.toEvent() =
    Event(
        id = this[EventTable.id],
        title = this[EventTable.title],
        start = this[EventTable.startAt].toKotlinInstant(),
        end = this[EventTable.endAt].toKotlinInstant(),
        createdAt = this[EventTable.createdAt].toKotlinInstant(),
        description = this[EventTable.description],
        ownerUserId = this[EventTable.ownerUserId],
    )
