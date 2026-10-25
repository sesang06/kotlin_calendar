package com.kotlincalendar.client.data.remote.http

import com.kotlincalendar.client.data.mapper.toWireInstant
import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventResponse
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

class HttpEventRemoteDataSourceTest {
    private val sampleProtoEvent =
        ProtoEvent(
            id = "evt-1",
            title = "Sample Event",
            start_time = Instant.fromEpochSeconds(1700000000, 0).toWireInstant(),
            end_time = Instant.fromEpochSeconds(1700003600, 0).toWireInstant(),
            description = "Test description",
            owner_user_id = "user-1",
        )

    @Test
    fun listEvents_decodesProtoResponse() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals("/api/events", request.url.encodedPath)
                    assertEquals("2026-10-25", request.url.parameters["date"])
                    assertEquals("Asia/Seoul", request.url.parameters["time_zone"])
                    respond(
                        content = ListEventsResponse(events = listOf(sampleProtoEvent)).encode(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/x-protobuf"),
                    )
                }

            val source = HttpEventRemoteDataSource(HttpClient(engine))
            val response =
                source.listEvents(
                    ListEventsRequest(date = "2026-10-25", time_zone = "Asia/Seoul"),
                )

            assertEquals(1, response.events.size)
            assertEquals("evt-1", response.events.first().id)
            assertEquals("Sample Event", response.events.first().title)
        }

    @Test
    fun getEvent_decodesProtoResponse() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals("/api/events/evt-1", request.url.encodedPath)
                    respond(
                        content = sampleProtoEvent.encode(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/x-protobuf"),
                    )
                }

            val source = HttpEventRemoteDataSource(HttpClient(engine))
            val response = source.getEvent(GetEventRequest(id = "evt-1"))

            assertEquals("evt-1", response.id)
            assertEquals("Sample Event", response.title)
        }

    @Test
    fun createEvent_encodesRequestAndDecodesResponse() =
        runTest {
            val createReq =
                CreateEventRequest(
                    title = "New Event",
                    start_time = Instant.fromEpochSeconds(1700000000, 0).toWireInstant(),
                    end_time = Instant.fromEpochSeconds(1700003600, 0).toWireInstant(),
                    description = "Description",
                )

            val engine =
                MockEngine { request ->
                    assertEquals("/api/events", request.url.encodedPath)
                    assertEquals("application/x-protobuf", request.body.contentType.toString())
                    respond(
                        content = sampleProtoEvent.encode(),
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/x-protobuf"),
                    )
                }

            val source = HttpEventRemoteDataSource(HttpClient(engine))
            val response = source.createEvent(createReq)

            assertEquals("evt-1", response.id)
        }

    @Test
    fun deleteEvent_sendsDeleteRequest() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals("/api/events/evt-to-delete", request.url.encodedPath)
                    respond(
                        content = ByteArray(0),
                        status = HttpStatusCode.NoContent,
                    )
                }

            val source = HttpEventRemoteDataSource(HttpClient(engine))
            val response = source.deleteEvent(DeleteEventRequest(id = "evt-to-delete"))

            assertEquals(DeleteEventResponse(), response)
        }
}
