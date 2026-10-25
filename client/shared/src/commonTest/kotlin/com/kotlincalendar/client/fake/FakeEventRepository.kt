package com.kotlincalendar.client.fake

import com.kotlincalendar.client.domain.model.Event
import com.kotlincalendar.client.domain.repository.EventRepository
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

class FakeEventRepository : EventRepository {
    var eventsResult: Result<List<Event>> = Result.success(emptyList())
    var getEventResult: Result<Event> = Result.failure(NoSuchElementException("Not found"))
    var createEventResult: Result<Event> = Result.failure(IllegalStateException("Failed"))
    var deleteEventResult: Result<Unit> = Result.success(Unit)

    val requestedDates = mutableListOf<LocalDate>()
    val requestedEventIds = mutableListOf<String>()
    val deletedEventIds = mutableListOf<String>()
    val createdEvents = mutableListOf<CreateEventCall>()

    data class CreateEventCall(
        val title: String,
        val startTime: Instant,
        val endTime: Instant,
        val description: String,
    )

    override suspend fun getEvents(
        date: LocalDate,
        timeZone: String?,
    ): Result<List<Event>> {
        requestedDates += date
        return eventsResult
    }

    override suspend fun getEvent(id: String): Result<Event> {
        requestedEventIds += id
        return getEventResult
    }

    override suspend fun createEvent(
        title: String,
        startTime: Instant,
        endTime: Instant,
        description: String,
    ): Result<Event> {
        createdEvents += CreateEventCall(title, startTime, endTime, description)
        return createEventResult
    }

    override suspend fun deleteEvent(id: String): Result<Unit> {
        deletedEventIds += id
        return deleteEventResult
    }
}
