package com.kotlincalendar.server.event.handler

import com.google.protobuf.Timestamp
import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.server.common.error.ValidationException
import com.kotlincalendar.server.event.domain.CreateEventCommand
import com.kotlincalendar.server.event.domain.Event
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import com.kotlincalendar.proto.calendar.v1.Event as EventMessage

fun Instant.toTimestamp(): Timestamp =
    Timestamp
        .newBuilder()
        .setSeconds(epochSeconds)
        .setNanos(nanosecondsOfSecond)
        .build()

fun Timestamp.toInstant(): Instant = Instant.fromEpochSeconds(seconds, nanos)

fun Event.toProto(): EventMessage =
    EventMessage
        .newBuilder()
        .setId(id)
        .setTitle(title)
        .setStartTime(start.toTimestamp())
        .setEndTime(end.toTimestamp())
        .setCreatedAt(createdAt.toTimestamp())
        .setDescription(description)
        .setOwnerUserId(ownerUserId)
        .build()

fun CreateEventRequest.toCommand(ownerUserId: String = ""): CreateEventCommand {
    if (!hasStartTime()) throw ValidationException("start_time is required")
    if (!hasEndTime()) throw ValidationException("end_time is required")
    return CreateEventCommand(
        title = title,
        start = startTime.toInstant(),
        end = endTime.toInstant(),
        description = description,
        ownerUserId = ownerUserId,
    )
}

fun parseDate(raw: String?): LocalDate {
    if (raw.isNullOrBlank()) {
        throw ValidationException("date is required")
    }
    return runCatching { LocalDate.parse(raw) }
        .getOrElse { throw ValidationException("invalid date '$raw' (expected yyyy-MM-dd)") }
}

fun parseTimeZone(raw: String?): TimeZone {
    if (raw.isNullOrBlank()) return TimeZone.UTC
    if (raw !in TimeZone.availableZoneIds) {
        throw ValidationException("invalid time_zone '$raw'")
    }
    return runCatching { TimeZone.of(raw) }
        .getOrElse { throw ValidationException("invalid time_zone '$raw'") }
}

fun requireId(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: throw ValidationException("id is required")
