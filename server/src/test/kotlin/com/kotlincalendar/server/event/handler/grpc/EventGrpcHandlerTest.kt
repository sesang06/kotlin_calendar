package com.kotlincalendar.server.event.handler.grpc

import com.google.protobuf.Timestamp
import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.EventApiGrpcKt
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.server.common.error.NotFoundException
import com.kotlincalendar.server.common.error.ValidationException
import com.kotlincalendar.server.common.grpc.AUTH_USER_ID_KEY
import com.kotlincalendar.server.event.domain.Event
import com.kotlincalendar.server.event.service.EventService
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class EventGrpcHandlerTest {
    private val service = mockk<EventService>()

    private lateinit var serverName: String
    private lateinit var server: io.grpc.Server
    private lateinit var channel: io.grpc.ManagedChannel
    private lateinit var stub: EventApiGrpcKt.EventApiCoroutineStub

    private var testUserId: String? = "user-1"

    private val authTestInterceptor =
        object : ServerInterceptor {
            override fun <ReqT, RespT> interceptCall(
                call: ServerCall<ReqT, RespT>,
                headers: Metadata,
                next: ServerCallHandler<ReqT, RespT>,
            ): ServerCall.Listener<ReqT> {
                val context =
                    if (testUserId != null) {
                        Context.current().withValue(AUTH_USER_ID_KEY, testUserId)
                    } else {
                        Context.current()
                    }
                return Contexts.interceptCall(context, call, headers, next)
            }
        }

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

    @BeforeEach
    fun setUp() {
        testUserId = "user-1"
        serverName = InProcessServerBuilder.generateName()
        server =
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(EventGrpcHandler(service), authTestInterceptor))
                .build()
                .start()
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
        stub = EventApiGrpcKt.EventApiCoroutineStub(channel)
    }

    @AfterEach
    fun tearDown() {
        channel.shutdownNow()
        server.shutdownNow()
    }

    @Test
    fun `인증되지 않은 요청은 UNAUTHENTICATED를 반환한다`() =
        runTest {
            testUserId = null

            val request =
                ListEventsRequest
                    .newBuilder()
                    .setDate("2026-10-26")
                    .build()

            val e =
                assertFailsWith<StatusException> {
                    stub.listEvents(request)
                }

            assertEquals(Status.Code.UNAUTHENTICATED, e.status.code)
            coVerify(exactly = 0) { service.getEventsOn(any(), any(), any()) }
        }

    @Test
    fun `listEvents는 요청 날짜의 일정 목록을 반환한다`() =
        runTest {
            coEvery { service.getEventsOn("user-1", any(), any()) } returns listOf(sampleEvent)

            val request =
                ListEventsRequest
                    .newBuilder()
                    .setDate("2026-10-26")
                    .setTimeZone("Asia/Seoul")
                    .build()
            val response = stub.listEvents(request)

            assertEquals(1, response.eventsCount)
            assertEquals("id-1", response.getEvents(0).id)
        }

    @Test
    fun `listEvents에 잘못된 날짜가 전달되면 INVALID_ARGUMENT`() =
        runTest {
            val request =
                ListEventsRequest
                    .newBuilder()
                    .setDate("invalid-date")
                    .build()

            val e =
                assertFailsWith<StatusException> {
                    stub.listEvents(request)
                }

            assertEquals(Status.Code.INVALID_ARGUMENT, e.status.code)
            coVerify(exactly = 0) { service.getEventsOn(any(), any(), any()) }
        }

    @Test
    fun `listEvents에 잘못된 타임존이 전달되면 INVALID_ARGUMENT`() =
        runTest {
            val request =
                ListEventsRequest
                    .newBuilder()
                    .setDate("2026-10-26")
                    .setTimeZone("Invalid/Zone")
                    .build()

            val e =
                assertFailsWith<StatusException> {
                    stub.listEvents(request)
                }

            assertEquals(Status.Code.INVALID_ARGUMENT, e.status.code)
            coVerify(exactly = 0) { service.getEventsOn(any(), any(), any()) }
        }

    @Test
    fun `getEvent는 존재하는 일정을 반환한다`() =
        runTest {
            coEvery { service.getEvent("user-1", "id-1") } returns sampleEvent

            val response = stub.getEvent(GetEventRequest.newBuilder().setId("id-1").build())

            assertEquals("id-1", response.id)
            assertEquals("회의", response.title)
        }

    @Test
    fun `getEvent에 없는 ID가 전달되면 NOT_FOUND`() =
        runTest {
            coEvery { service.getEvent("user-1", "x") } throws NotFoundException("event x not found")

            val e =
                assertFailsWith<StatusException> {
                    stub.getEvent(GetEventRequest.newBuilder().setId("x").build())
                }

            assertEquals(Status.Code.NOT_FOUND, e.status.code)
        }

    @Test
    fun `createEvent는 생성된 일정을 반환한다`() =
        runTest {
            coEvery { service.createEvent(any()) } returns sampleEvent

            val request =
                CreateEventRequest
                    .newBuilder()
                    .setTitle("회의")
                    .setStartTime(Timestamp.newBuilder().setSeconds(1792976400).build())
                    .setEndTime(Timestamp.newBuilder().setSeconds(1792980000).build())
                    .setDescription("설명")
                    .build()
            val response = stub.createEvent(request)

            assertEquals("id-1", response.id)
            assertEquals("회의", response.title)
        }

    @Test
    fun `createEvent 도메인 검증 실패 시 INVALID_ARGUMENT`() =
        runTest {
            coEvery { service.createEvent(any()) } throws ValidationException("end must be after start")

            val request =
                CreateEventRequest
                    .newBuilder()
                    .setTitle("회의")
                    .setStartTime(Timestamp.newBuilder().setSeconds(1792980000).build())
                    .setEndTime(Timestamp.newBuilder().setSeconds(1792976400).build())
                    .build()

            val e =
                assertFailsWith<StatusException> {
                    stub.createEvent(request)
                }

            assertEquals(Status.Code.INVALID_ARGUMENT, e.status.code)
        }

    @Test
    fun `deleteEvent는 일정을 삭제하고 빈 응답을 반환한다`() =
        runTest {
            coEvery { service.deleteEvent("user-1", "id-1") } returns Unit

            val response = stub.deleteEvent(DeleteEventRequest.newBuilder().setId("id-1").build())

            assertEquals(
                com.kotlincalendar.proto.calendar.v1.DeleteEventResponse
                    .getDefaultInstance(),
                response,
            )
            coVerify(exactly = 1) { service.deleteEvent("user-1", "id-1") }
        }

    @Test
    fun `deleteEvent에 없는 ID가 전달되면 NOT_FOUND`() =
        runTest {
            coEvery { service.deleteEvent("user-1", "x") } throws NotFoundException("event x not found")

            val e =
                assertFailsWith<StatusException> {
                    stub.deleteEvent(DeleteEventRequest.newBuilder().setId("x").build())
                }

            assertEquals(Status.Code.NOT_FOUND, e.status.code)
        }
}
