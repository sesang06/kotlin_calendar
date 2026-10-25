package com.kotlincalendar.client.data.remote.grpc

import com.kotlincalendar.client.data.remote.EventRemoteDataSource
import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventResponse
import com.kotlincalendar.proto.calendar.v1.EventApiClient
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

class GrpcEventRemoteDataSource(
    private val api: EventApiClient,
) : EventRemoteDataSource {
    override suspend fun listEvents(request: ListEventsRequest): ListEventsResponse = api.ListEvents().execute(request)

    override suspend fun getEvent(request: GetEventRequest): ProtoEvent = api.GetEvent().execute(request)

    override suspend fun createEvent(request: CreateEventRequest): ProtoEvent = api.CreateEvent().execute(request)

    override suspend fun deleteEvent(request: DeleteEventRequest): DeleteEventResponse = api.DeleteEvent().execute(request)
}
