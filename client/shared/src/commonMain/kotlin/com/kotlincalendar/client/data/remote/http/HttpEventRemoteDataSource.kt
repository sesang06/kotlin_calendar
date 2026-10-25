package com.kotlincalendar.client.data.remote.http

import com.kotlincalendar.client.data.remote.EventRemoteDataSource
import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventResponse
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

class HttpEventRemoteDataSource(
    private val client: HttpClient,
) : EventRemoteDataSource {
    override suspend fun listEvents(request: ListEventsRequest): ListEventsResponse {
        val bytes =
            client
                .get("/api/events") {
                    parameter("date", request.date)
                    if (request.time_zone.isNotBlank()) {
                        parameter("time_zone", request.time_zone)
                    }
                }.body<ByteArray>()
        return ListEventsResponse.ADAPTER.decode(bytes)
    }

    override suspend fun getEvent(request: GetEventRequest): ProtoEvent {
        val bytes = client.get("/api/events/${request.id}").body<ByteArray>()
        return ProtoEvent.ADAPTER.decode(bytes)
    }

    override suspend fun createEvent(request: CreateEventRequest): ProtoEvent {
        val bytes =
            client
                .post("/api/events") {
                    contentType(ContentType("application", "x-protobuf"))
                    setBody(request.encode())
                }.body<ByteArray>()
        return ProtoEvent.ADAPTER.decode(bytes)
    }

    override suspend fun deleteEvent(request: DeleteEventRequest): DeleteEventResponse {
        client.delete("/api/events/${request.id}")
        return DeleteEventResponse()
    }
}
