package com.kotlincalendar.client.data.repository

import com.kotlincalendar.client.common.runCatchingCancellable
import com.kotlincalendar.client.data.mapper.toCreateEventRequest
import com.kotlincalendar.client.data.mapper.toDomain
import com.kotlincalendar.client.data.mapper.toUpdateEventRequest
import com.kotlincalendar.client.data.remote.EventRemoteDataSource
import com.kotlincalendar.client.domain.model.Event
import com.kotlincalendar.client.domain.repository.EventRepository
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsInRangeRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

class EventRepositoryImpl(
    private val remote: EventRemoteDataSource,
) : EventRepository {
    override suspend fun getEvents(
        date: LocalDate,
        timeZone: String?,
    ): Result<List<Event>> =
        runCatchingCancellable {
            remote
                .listEvents(
                    ListEventsRequest(
                        date = date.toString(),
                        time_zone = timeZone ?: "",
                    ),
                ).events
                .map { it.toDomain() }
        }

    override suspend fun getEventsInRange(
        startDate: LocalDate,
        endDate: LocalDate,
        timeZone: String?,
    ): Result<List<Event>> =
        runCatchingCancellable {
            remote
                .listEventsInRange(
                    ListEventsInRangeRequest(
                        start_date = startDate.toString(),
                        end_date = endDate.toString(),
                        time_zone = timeZone ?: "",
                    ),
                ).events
                .map { it.toDomain() }
        }

    override suspend fun getEvent(id: String): Result<Event> =
        runCatchingCancellable {
            remote.getEvent(GetEventRequest(id = id)).toDomain()
        }

    override suspend fun createEvent(
        title: String,
        startTime: Instant,
        endTime: Instant,
        description: String,
    ): Result<Event> =
        runCatchingCancellable {
            remote
                .createEvent(
                    toCreateEventRequest(
                        title = title,
                        startTime = startTime,
                        endTime = endTime,
                        description = description,
                    ),
                ).toDomain()
        }

    override suspend fun updateEvent(
        id: String,
        title: String,
        startTime: Instant,
        endTime: Instant,
        description: String,
    ): Result<Event> =
        runCatchingCancellable {
            remote
                .updateEvent(
                    toUpdateEventRequest(
                        id = id,
                        title = title,
                        startTime = startTime,
                        endTime = endTime,
                        description = description,
                    ),
                ).toDomain()
        }

    override suspend fun deleteEvent(id: String): Result<Unit> =
        runCatchingCancellable {
            remote.deleteEvent(DeleteEventRequest(id = id))
            Unit
        }
}
