package com.kotlincalendar.server.event.service

import com.kotlincalendar.server.common.error.NotFoundException
import com.kotlincalendar.server.common.error.ValidationException
import com.kotlincalendar.server.event.domain.CreateEventCommand
import com.kotlincalendar.server.event.domain.Event
import com.kotlincalendar.server.event.domain.UpdateEventCommand
import com.kotlincalendar.server.event.repository.EventRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

class EventService(
    private val eventRepository: EventRepository,
    private val clock: Clock = Clock.System,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    suspend fun getEventsOn(
        userId: String,
        date: LocalDate,
        timeZone: TimeZone = TimeZone.UTC,
    ): List<Event> {
        val dayStart = date.atStartOfDayIn(timeZone)
        val dayEnd = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        return eventRepository.findOverlapping(userId, dayStart, dayEnd)
    }

    /** [startDate]~[endDate](포함) 구간과 겹치는 일정. 구간은 최대 [MAX_RANGE_DAYS]일. */
    suspend fun getEventsInRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        timeZone: TimeZone = TimeZone.UTC,
    ): List<Event> {
        if (endDate < startDate) {
            throw ValidationException("end_date must not be before start_date")
        }
        if (startDate.daysUntil(endDate) > MAX_RANGE_DAYS) {
            throw ValidationException("date range must be at most $MAX_RANGE_DAYS days")
        }
        val rangeStart = startDate.atStartOfDayIn(timeZone)
        val rangeEnd = endDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        return eventRepository.findOverlapping(userId, rangeStart, rangeEnd)
    }

    suspend fun getEvent(
        userId: String,
        id: String,
    ): Event =
        eventRepository.findByIdAndOwnerUserId(id, userId)
            ?: throw NotFoundException("event $id not found")

    suspend fun createEvent(command: CreateEventCommand): Event {
        if (command.ownerUserId.isBlank()) {
            throw ValidationException("ownerUserId must not be blank")
        }
        validateEventFields(command.title, command.description, command.start, command.end)

        val event =
            Event(
                id = idGenerator(),
                title = command.title.trim(),
                start = command.start,
                end = command.end,
                createdAt = clock.now(),
                description = command.description,
                ownerUserId = command.ownerUserId,
            )
        return eventRepository.save(event)
    }

    suspend fun updateEvent(command: UpdateEventCommand): Event {
        validateEventFields(command.title, command.description, command.start, command.end)
        val existing =
            eventRepository.findByIdAndOwnerUserId(command.id, command.ownerUserId)
                ?: throw NotFoundException("event ${command.id} not found")
        val updated =
            existing.copy(
                title = command.title.trim(),
                start = command.start,
                end = command.end,
                description = command.description,
            )
        return eventRepository.save(updated)
    }

    suspend fun deleteEvent(
        userId: String,
        id: String,
    ) {
        if (!eventRepository.deleteByIdAndOwnerUserId(id, userId)) {
            throw NotFoundException("event $id not found")
        }
    }

    private fun validateEventFields(
        title: String,
        description: String,
        start: Instant,
        end: Instant,
    ) {
        if (title.isBlank()) {
            throw ValidationException("title must not be blank")
        }
        val titleCodePoints = title.codePointCount(0, title.length)
        if (titleCodePoints < 1 || titleCodePoints > 200) {
            throw ValidationException("title length must be between 1 and 200")
        }
        val descCodePoints = description.codePointCount(0, description.length)
        if (descCodePoints > 2000) {
            throw ValidationException("description length must be at most 2000")
        }
        if (end <= start) {
            throw ValidationException("end must be after start")
        }
    }

    companion object {
        const val MAX_RANGE_DAYS = 62
    }
}
