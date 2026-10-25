package com.kotlincalendar.server.event.repository

import com.kotlincalendar.server.event.domain.Event
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import kotlin.time.Instant

/** 시각은 벤더 중립을 위해 UTC epoch millis(BIGINT)로 저장한다. */
object EventTable : Table("events") {
    val id = varchar("id", 36)
    val title = varchar("title", 200)
    val startAt = long("start_at")
    val endAt = long("end_at")
    val createdAt = long("created_at")
    val description = varchar("description", 2000).default("")
    val ownerUserId = varchar("owner_user_id", 36).default("")

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, startAt, endAt)
    }
}

fun ResultRow.toEvent() =
    Event(
        id = this[EventTable.id],
        title = this[EventTable.title],
        start = Instant.fromEpochMilliseconds(this[EventTable.startAt]),
        end = Instant.fromEpochMilliseconds(this[EventTable.endAt]),
        createdAt = Instant.fromEpochMilliseconds(this[EventTable.createdAt]),
        description = this[EventTable.description],
        ownerUserId = this[EventTable.ownerUserId],
    )
