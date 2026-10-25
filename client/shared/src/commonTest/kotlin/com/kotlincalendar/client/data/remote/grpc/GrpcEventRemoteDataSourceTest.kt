package com.kotlincalendar.client.data.remote.grpc

import com.kotlincalendar.client.fake.FakeAuthSession
import com.kotlincalendar.client.fake.FakeEventApiClient
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

class GrpcEventRemoteDataSourceTest {
    private val api = FakeEventApiClient()
    private val session = FakeAuthSession(token = "access-1")
    private val source = GrpcEventRemoteDataSource(api, session)

    @Test
    fun listEvents_sendsAuthorizationMetadata() =
        runTest {
            api.handler = { _, _ -> ListEventsResponse(events = listOf(ProtoEvent(id = "evt-1"))) }

            val response = source.listEvents(ListEventsRequest(date = "2026-10-25"))

            assertEquals("evt-1", response.events.single().id)
            assertEquals(listOf(mapOf("authorization" to "Bearer access-1")), api.executedMetadata)
        }

    @Test
    fun deleteEvent_sendsAuthorizationMetadata() =
        runTest {
            source.deleteEvent(DeleteEventRequest(id = "evt-1"))

            assertEquals("Bearer access-1", api.executedMetadata.single()["authorization"])
        }

    @Test
    fun unauthenticated_refreshesOnceAndRetriesWithNewCall() =
        runTest {
            api.handler = { metadata, _ ->
                if (metadata["authorization"] == "Bearer access-1") {
                    throw GrpcException(GrpcStatus.UNAUTHENTICATED, "expired")
                }
                ProtoEvent(id = "evt-1")
            }

            val response = source.getEvent(GetEventRequest(id = "evt-1"))

            assertEquals("evt-1", response.id)
            assertEquals(listOf<String?>("access-1"), session.rejectedTokens)
            assertEquals(
                listOf("Bearer access-1", "Bearer access-2"),
                api.executedMetadata.map { it["authorization"] },
            )
        }

    @Test
    fun unauthenticated_refreshFails_throwsOriginalErrorWithoutRetry() =
        runTest {
            session.refreshResult = Result.failure(IllegalStateException("refresh rejected"))
            api.handler = { _, _ -> throw GrpcException(GrpcStatus.UNAUTHENTICATED, "expired") }

            val error = assertFailsWith<GrpcException> { source.getEvent(GetEventRequest(id = "evt-1")) }

            assertEquals(GrpcStatus.UNAUTHENTICATED, error.grpcStatus)
            assertTrue(error.suppressedExceptions.any { it.message == "refresh rejected" })
            assertEquals(1, api.executedMetadata.size)
        }

    @Test
    fun unauthenticatedAgainAfterRefresh_doesNotRetryTwice() =
        runTest {
            api.handler = { _, _ -> throw GrpcException(GrpcStatus.UNAUTHENTICATED, "expired") }

            assertFailsWith<GrpcException> { source.getEvent(GetEventRequest(id = "evt-1")) }

            assertEquals(2, api.executedMetadata.size)
            assertEquals(1, session.rejectedTokens.size)
        }

    @Test
    fun otherGrpcError_doesNotRefresh() =
        runTest {
            api.handler = { _, _ -> throw GrpcException(GrpcStatus.NOT_FOUND, "missing") }

            assertFailsWith<GrpcException> { source.getEvent(GetEventRequest(id = "evt-1")) }

            assertTrue(session.rejectedTokens.isEmpty())
            assertEquals(1, api.executedMetadata.size)
        }
}
