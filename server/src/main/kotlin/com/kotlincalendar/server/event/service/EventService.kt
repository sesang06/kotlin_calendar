package com.kotlincalendar.server.event.service

import com.kotlincalendar.server.common.error.NotFoundException
import com.kotlincalendar.server.common.error.ValidationException
import com.kotlincalendar.server.event.domain.CreateEventCommand
import com.kotlincalendar.server.event.domain.Event
import com.kotlincalendar.server.event.repository.EventRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import java.util.UUID
import kotlin.time.Clock

class EventService(
    private val eventRepository: EventRepository,
    private val clock: Clock = Clock.System,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    suspend fun getEventsOn(
        date: LocalDate,
        timeZone: TimeZone = TimeZone.UTC,
    ): List<Event> {
        val dayStart = date.atStartOfDayIn(timeZone)
        val dayEnd = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        return eventRepository.findOverlapping(dayStart, dayEnd)
    }

    suspend fun getEvent(id: String): Event = eventRepository.findById(id) ?: throw NotFoundException("event $id not found")

    suspend fun createEvent(command: CreateEventCommand): Event {
        if (command.title.isBlank()) {
            throw ValidationException("title must not be blank")
        }
        val titleCodePoints = command.title.codePointCount(0, command.title.length)
        if (titleCodePoints < 1 || titleCodePoints > 200) {
            throw ValidationException("title length must be between 1 and 200")
        }
        val descCodePoints = command.description.codePointCount(0, command.description.length)
        if (descCodePoints > 2000) {
            throw ValidationException("description length must be at most 2000")
        }
        if (command.end <= command.start) {
            throw ValidationException("end must be after start")
        }

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

    suspend fun deleteEvent(id: String) {
        if (!eventRepository.deleteById(id)) {
            throw NotFoundException("event $id not found")
        }
    }
}
