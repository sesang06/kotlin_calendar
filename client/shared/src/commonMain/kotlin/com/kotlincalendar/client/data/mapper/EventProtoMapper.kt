package com.kotlincalendar.client.data.mapper

import com.kotlincalendar.client.domain.model.Event
import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import kotlin.time.Instant
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

fun ProtoEvent.toDomain(): Event =
    Event(
        id = id,
        title = title,
        start = start_time?.toKotlinInstant() ?: Instant.DISTANT_PAST,
        end = end_time?.toKotlinInstant() ?: Instant.DISTANT_PAST,
        createdAt = created_at?.toKotlinInstant(),
        description = description,
        ownerUserId = owner_user_id,
    )

fun com.squareup.wire.Instant.toKotlinInstant(): Instant = Instant.fromEpochSeconds(epochSecond, nano.toLong())

fun Instant.toWireInstant(): com.squareup.wire.Instant =
    com.squareup.wire.Instant
        .ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())

fun toCreateEventRequest(
    title: String,
    startTime: Instant,
    endTime: Instant,
    description: String = "",
): CreateEventRequest =
    CreateEventRequest(
        title = title,
        start_time = startTime.toWireInstant(),
        end_time = endTime.toWireInstant(),
        description = description,
    )
