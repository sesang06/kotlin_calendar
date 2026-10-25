package com.kotlincalendar.client.data.repository

import com.kotlincalendar.client.data.mapper.toWireInstant
import com.kotlincalendar.client.fake.FakeEventRemoteDataSource
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

class EventRepositoryImplTest {
    private val fakeRemote = FakeEventRemoteDataSource()
    private val repository = EventRepositoryImpl(fakeRemote)

    private val startTime = Instant.fromEpochSeconds(1700000000, 0)
    private val endTime = Instant.fromEpochSeconds(1700003600, 0)

    @Test
    fun getEvents_returnsMappedDomainEventsOnSuccess() =
        runTest {
            val proto =
                ProtoEvent(
                    id = "e1",
                    title = "Test",
                    start_time = startTime.toWireInstant(),
                    end_time = endTime.toWireInstant(),
                )
            fakeRemote.listEventsResponse = ListEventsResponse(events = listOf(proto))

            val result = repository.getEvents(LocalDate(2026, 10, 25), "Asia/Seoul")

            assertTrue(result.isSuccess)
            val events = result.getOrThrow()
            assertEquals(1, events.size)
            assertEquals("e1", events.first().id)
            assertEquals("Test", events.first().title)
            assertEquals("2026-10-25", fakeRemote.receivedListRequests.first().date)
            assertEquals("Asia/Seoul", fakeRemote.receivedListRequests.first().time_zone)
        }

    @Test
    fun getEvents_capturesExceptionIntoResult() =
        runTest {
            fakeRemote.shouldThrow = IllegalStateException("Network error")

            val result = repository.getEvents(LocalDate(2026, 10, 25))

            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)
        }

    @Test
    fun getEvent_returnsDomainEventOnSuccess() =
        runTest {
            fakeRemote.getEventResponse =
                ProtoEvent(
                    id = "e1",
                    title = "Meeting",
                    start_time = startTime.toWireInstant(),
                    end_time = endTime.toWireInstant(),
                )

            val result = repository.getEvent("e1")

            assertTrue(result.isSuccess)
            val event = result.getOrThrow()
            assertEquals("e1", event.id)
            assertEquals("Meeting", event.title)
            assertEquals("e1", fakeRemote.receivedGetRequests.first().id)
        }

    @Test
    fun createEvent_sendsRequestAndReturnsCreatedDomainEvent() =
        runTest {
            fakeRemote.createEventResponse =
                ProtoEvent(
                    id = "new-id",
                    title = "Created",
                    start_time = startTime.toWireInstant(),
                    end_time = endTime.toWireInstant(),
                )

            val result =
                repository.createEvent(
                    title = "Created",
                    startTime = startTime,
                    endTime = endTime,
                    description = "desc",
                )

            assertTrue(result.isSuccess)
            assertEquals("new-id", result.getOrThrow().id)
            val req = fakeRemote.receivedCreateRequests.first()
            assertEquals("Created", req.title)
            assertEquals("desc", req.description)
        }

    @Test
    fun deleteEvent_callsRemoteDelete() =
        runTest {
            val result = repository.deleteEvent("del-id")

            assertTrue(result.isSuccess)
            assertEquals("del-id", fakeRemote.receivedDeleteRequests.first().id)
        }

    @Test
    fun updateEvent_sendsRequestWithIdAndReturnsDomainEvent() =
        runTest {
            fakeRemote.updateEventResponse =
                ProtoEvent(
                    id = "e1",
                    title = "Updated",
                    start_time = startTime.toWireInstant(),
                    end_time = endTime.toWireInstant(),
                )

            val result = repository.updateEvent("e1", "Updated", startTime, endTime, "memo")

            assertEquals("Updated", result.getOrThrow().title)
            val req = fakeRemote.receivedUpdateRequests.single()
            assertEquals("e1", req.id)
            assertEquals("Updated", req.title)
            assertEquals(startTime.toWireInstant(), req.start_time)
            assertEquals("memo", req.description)
        }
}
