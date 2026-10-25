package com.kotlincalendar.client.domain.repository

import com.kotlincalendar.client.domain.model.Event
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

interface EventRepository {
    suspend fun getEvents(
        date: LocalDate,
        timeZone: String? = null,
    ): Result<List<Event>>

    suspend fun getEvent(id: String): Result<Event>

    suspend fun createEvent(
        title: String,
        startTime: Instant,
        endTime: Instant,
        description: String = "",
    ): Result<Event>

    /** 일정 전체 수정. */
    suspend fun updateEvent(
        id: String,
        title: String,
        startTime: Instant,
        endTime: Instant,
        description: String = "",
    ): Result<Event>

    suspend fun deleteEvent(id: String): Result<Unit>
}
