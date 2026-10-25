package com.kotlincalendar.client.data.remote.http

import com.kotlincalendar.client.data.remote.EventRemoteDataSource
import com.kotlincalendar.client.data.remote.auth.AuthSession
import com.kotlincalendar.client.data.remote.auth.withAuthRetry
import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventResponse
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import com.kotlincalendar.proto.calendar.v1.UpdateEventRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

class HttpEventRemoteDataSource(
    private val client: HttpClient,
    private val authSession: AuthSession,
) : EventRemoteDataSource {
    override suspend fun listEvents(request: ListEventsRequest): ListEventsResponse {
        val bytes =
            authSession.withAuthRetry { token ->
                client
                    .get("/api/events") {
                        bearer(token)
                        parameter("date", request.date)
                        if (request.time_zone.isNotBlank()) {
                            parameter("time_zone", request.time_zone)
                        }
                    }.body<ByteArray>()
            }
        return ListEventsResponse.ADAPTER.decode(bytes)
    }

    override suspend fun getEvent(request: GetEventRequest): ProtoEvent {
        val bytes =
            authSession.withAuthRetry { token ->
                client.get("/api/events/${request.id}") { bearer(token) }.body<ByteArray>()
            }
        return ProtoEvent.ADAPTER.decode(bytes)
    }

    override suspend fun createEvent(request: CreateEventRequest): ProtoEvent {
        val bytes =
            authSession.withAuthRetry { token ->
                client
                    .post("/api/events") {
                        bearer(token)
                        contentType(ContentType("application", "x-protobuf"))
                        setBody(request.encode())
                    }.body<ByteArray>()
            }
        return ProtoEvent.ADAPTER.decode(bytes)
    }

    override suspend fun updateEvent(request: UpdateEventRequest): ProtoEvent {
        val bytes =
            authSession.withAuthRetry { token ->
                client
                    .patch("/api/events/${request.id}") {
                        bearer(token)
                        contentType(ContentType("application", "x-protobuf"))
                        setBody(request.encode())
                    }.body<ByteArray>()
            }
        return ProtoEvent.ADAPTER.decode(bytes)
    }

    override suspend fun deleteEvent(request: DeleteEventRequest): DeleteEventResponse {
        authSession.withAuthRetry { token ->
            client.delete("/api/events/${request.id}") { bearer(token) }
        }
        return DeleteEventResponse()
    }

    private fun HttpRequestBuilder.bearer(token: String?) {
        if (token != null) bearerAuth(token)
    }
}
