package com.kotlincalendar.client.data.remote.http

import com.kotlincalendar.client.data.remote.ApiConfig
import com.kotlincalendar.client.fake.FakeAuthSession
import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

/** 실제 [createHttpClient] 설정(expectSuccess = true)으로 Authorization 헤더와 401 재시도를 검증한다. */
class HttpEventRemoteDataSourceAuthTest {
    private val session = FakeAuthSession(token = "access-1")
    private val authHeaders = mutableListOf<String?>()

    private fun source(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpEventRemoteDataSource {
        val engine =
            MockEngine { request ->
                authHeaders += request.headers[HttpHeaders.Authorization]
                handler(request)
            }
        return HttpEventRemoteDataSource(createHttpClient(engine, ApiConfig()), session)
    }

    private fun MockRequestHandleScope.protoOk(content: ByteArray) =
        respond(
            content = content,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/x-protobuf"),
        )

    private fun MockRequestHandleScope.unauthorized() = respond(content = ByteArray(0), status = HttpStatusCode.Unauthorized)

    @Test
    fun everyEventCall_sendsBearerToken() =
        runTest {
            val source =
                source { request ->
                    when {
                        request.url.encodedPath == "/api/events" && request.method.value == "GET" ->
                            protoOk(ListEventsResponse().encode())
                        request.method.value == "DELETE" -> respond(ByteArray(0), HttpStatusCode.NoContent)
                        else -> protoOk(ProtoEvent(id = "evt-1").encode())
                    }
                }

            source.listEvents(ListEventsRequest(date = "2026-10-25"))
            source.getEvent(GetEventRequest(id = "evt-1"))
            source.createEvent(CreateEventRequest(title = "t"))
            source.deleteEvent(DeleteEventRequest(id = "evt-1"))

            assertEquals(List<String?>(4) { "Bearer access-1" }, authHeaders)
        }

    @Test
    fun noToken_sendsNoAuthorizationHeader() =
        runTest {
            session.token = null
            val source = source { protoOk(ListEventsResponse().encode()) }

            source.listEvents(ListEventsRequest(date = "2026-10-25"))

            assertNull(authHeaders.single())
        }

    @Test
    fun unauthorized_refreshesOnceAndRetriesWithNewToken() =
        runTest {
            val source =
                source { request ->
                    if (request.headers[HttpHeaders.Authorization] == "Bearer access-1") {
                        unauthorized()
                    } else {
                        protoOk(ProtoEvent(id = "evt-1").encode())
                    }
                }

            val event = source.getEvent(GetEventRequest(id = "evt-1"))

            assertEquals("evt-1", event.id)
            assertEquals(listOf<String?>("access-1"), session.rejectedTokens)
            assertEquals(listOf<String?>("Bearer access-1", "Bearer access-2"), authHeaders)
        }

    @Test
    fun unauthorized_refreshFails_throwsUnauthorizedWithoutRetry() =
        runTest {
            session.refreshResult = Result.failure(IllegalStateException("refresh rejected"))
            val source = source { unauthorized() }

            val error =
                assertFailsWith<ClientRequestException> {
                    source.listEvents(ListEventsRequest(date = "2026-10-25"))
                }

            assertEquals(HttpStatusCode.Unauthorized, error.response.status)
            assertTrue(error.suppressedExceptions.any { it.message == "refresh rejected" })
            assertEquals(1, authHeaders.size)
        }

    @Test
    fun unauthorizedAgainAfterRefresh_doesNotLoop() =
        runTest {
            val source = source { unauthorized() }

            assertFailsWith<ClientRequestException> { source.deleteEvent(DeleteEventRequest(id = "evt-1")) }

            assertEquals(listOf<String?>("Bearer access-1", "Bearer access-2"), authHeaders)
            assertEquals(1, session.rejectedTokens.size)
        }

    @Test
    fun otherClientError_doesNotRefresh() =
        runTest {
            val source = source { respond(ByteArray(0), HttpStatusCode.NotFound) }

            assertFailsWith<ClientRequestException> { source.getEvent(GetEventRequest(id = "missing")) }

            assertTrue(session.rejectedTokens.isEmpty())
            assertEquals(1, authHeaders.size)
        }
}
