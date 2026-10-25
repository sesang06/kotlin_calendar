package com.kotlincalendar.server.event.handler.http

import com.google.protobuf.Timestamp
import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.ErrorResponse
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import com.kotlincalendar.server.common.auth.TokenProvider
import com.kotlincalendar.server.common.error.NotFoundException
import com.kotlincalendar.server.common.http.ProtobufContentType
import com.kotlincalendar.server.event.domain.Event
import com.kotlincalendar.server.event.service.EventService
import com.kotlincalendar.server.plugins.configureKoin
import com.kotlincalendar.server.plugins.configureRouting
import com.kotlincalendar.server.plugins.configureStatusPages
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import com.kotlincalendar.proto.calendar.v1.Event as EventMessage

class EventHttpHandlerTest {
    private val service = mockk<EventService>()
    private val tokenProvider = mockk<TokenProvider>()

    private val sampleEvent =
        Event(
            id = "id-1",
            title = "회의",
            start = Instant.parse("2026-10-26T01:00:00Z"),
            end = Instant.parse("2026-10-26T02:00:00Z"),
            createdAt = Instant.parse("2026-10-25T00:00:00Z"),
            description = "설명",
            ownerUserId = "user-1",
        )

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            environment { config = MapApplicationConfig() }
            application {
                configureKoin(
                    listOf(
                        module {
                            single { service }
                            single { tokenProvider }
                            single { EventHttpHandler(get(), get()) }
                        },
                    ),
                )
                configureStatusPages()
                configureRouting()
            }
            every { tokenProvider.verifyAccessToken("valid-token", any()) } returns "user-1"
            every { tokenProvider.verifyAccessToken("invalid-token", any()) } returns null
            block()
        }

    @Test
    fun `인증 헤더가 없으면 401을 반환한다`() =
        withApp {
            val response = client.get("/api/events?date=2026-10-26")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `유효하지 않은 토큰이면 401을 반환한다`() =
        withApp {
            val response =
                client.get("/api/events?date=2026-10-26") {
                    header(HttpHeaders.Authorization, "Bearer invalid-token")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `POST events는 201과 생성된 일정을 proto로 반환한다`() =
        withApp {
            coEvery { service.createEvent(any()) } returns sampleEvent

            val request =
                CreateEventRequest
                    .newBuilder()
                    .setTitle("회의")
                    .setStartTime(Timestamp.newBuilder().setSeconds(1792976400).build())
                    .setEndTime(Timestamp.newBuilder().setSeconds(1792980000).build())
                    .setDescription("설명")
                    .build()

            val response =
                client.post("/api/events") {
                    header(HttpHeaders.Authorization, "Bearer valid-token")
                    header(HttpHeaders.ContentType, ProtobufContentType.toString())
                    setBody(request.toByteArray())
                }

            assertEquals(HttpStatusCode.Created, response.status)
            val responseEvent = EventMessage.parseFrom(response.body<ByteArray>())
            assertEquals("id-1", responseEvent.id)
            assertEquals("회의", responseEvent.title)
        }

    @Test
    fun `GET events는 200과 일정 목록을 proto로 반환한다`() =
        withApp {
            coEvery { service.getEventsOn("user-1", any(), any()) } returns listOf(sampleEvent)

            val response =
                client.get("/api/events?date=2026-10-26&time_zone=Asia/Seoul") {
                    header(HttpHeaders.Authorization, "Bearer valid-token")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val listResponse = ListEventsResponse.parseFrom(response.body<ByteArray>())
            assertEquals(1, listResponse.eventsCount)
            assertEquals("id-1", listResponse.getEvents(0).id)
        }

    @Test
    fun `date 형식이 잘못되면 400이고 Service를 호출하지 않는다`() =
        withApp {
            val response =
                client.get("/api/events?date=invalid-date") {
                    header(HttpHeaders.Authorization, "Bearer valid-token")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val errorResponse = ErrorResponse.parseFrom(response.body<ByteArray>())
            assertEquals("invalid date 'invalid-date' (expected yyyy-MM-dd)", errorResponse.message)
            coVerify(exactly = 0) { service.getEventsOn(any(), any(), any()) }
        }

    @Test
    fun `time_zone 형식이 잘못되면 400이고 Service를 호출하지 않는다`() =
        withApp {
            val response =
                client.get("/api/events?date=2026-10-26&time_zone=Invalid/Zone") {
                    header(HttpHeaders.Authorization, "Bearer valid-token")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val errorResponse = ErrorResponse.parseFrom(response.body<ByteArray>())
            assertEquals("invalid time_zone 'Invalid/Zone'", errorResponse.message)
            coVerify(exactly = 0) { service.getEventsOn(any(), any(), any()) }
        }

    @Test
    fun `GET events id는 200과 일정을 proto로 반환한다`() =
        withApp {
            coEvery { service.getEvent("user-1", "id-1") } returns sampleEvent

            val response =
                client.get("/api/events/id-1") {
                    header(HttpHeaders.Authorization, "Bearer valid-token")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val eventMessage = EventMessage.parseFrom(response.body<ByteArray>())
            assertEquals("id-1", eventMessage.id)
        }

    @Test
    fun `존재하지 않는 event ID로 GET하면 404와 ErrorResponse를 반환한다`() =
        withApp {
            coEvery { service.getEvent("user-1", "x") } throws NotFoundException("event x not found")

            val response =
                client.get("/api/events/x") {
                    header(HttpHeaders.Authorization, "Bearer valid-token")
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
            val errorResponse = ErrorResponse.parseFrom(response.body<ByteArray>())
            assertEquals("event x not found", errorResponse.message)
        }

    @Test
    fun `DELETE events id는 204 NoContent를 반환한다`() =
        withApp {
            coEvery { service.deleteEvent("user-1", "id-1") } returns Unit

            val response =
                client.delete("/api/events/id-1") {
                    header(HttpHeaders.Authorization, "Bearer valid-token")
                }

            assertEquals(HttpStatusCode.NoContent, response.status)
            coVerify(exactly = 1) { service.deleteEvent("user-1", "id-1") }
        }

    @Test
    fun `존재하지 않는 event ID로 DELETE하면 404와 ErrorResponse를 반환한다`() =
        withApp {
            coEvery { service.deleteEvent("user-1", "x") } throws NotFoundException("event x not found")

            val response =
                client.delete("/api/events/x") {
                    header(HttpHeaders.Authorization, "Bearer valid-token")
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
            val errorResponse = ErrorResponse.parseFrom(response.body<ByteArray>())
            assertEquals("event x not found", errorResponse.message)
        }
}
