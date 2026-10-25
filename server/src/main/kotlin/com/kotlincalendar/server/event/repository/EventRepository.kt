package com.kotlincalendar.server.event.repository

import com.kotlincalendar.server.event.domain.Event
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

interface EventRepository {
    suspend fun findById(id: String): Event?

    suspend fun findByIdAndOwnerUserId(
        id: String,
        ownerUserId: String,
    ): Event?

    suspend fun findByDate(
        ownerUserId: String,
        date: LocalDate,
    ): List<Event>

    suspend fun findOverlapping(
        ownerUserId: String,
        start: Instant,
        end: Instant,
    ): List<Event>

    suspend fun existsOverlapping(
        ownerUserId: String,
        start: Instant,
        end: Instant,
    ): Boolean

    suspend fun save(event: Event): Event

    suspend fun deleteById(id: String): Boolean

    suspend fun deleteByIdAndOwnerUserId(
        id: String,
        ownerUserId: String,
    ): Boolean
}
