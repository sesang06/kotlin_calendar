package com.kotlincalendar.client.data.remote

import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventResponse
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import com.kotlincalendar.proto.calendar.v1.UpdateEventRequest
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

interface EventRemoteDataSource {
    suspend fun listEvents(request: ListEventsRequest): ListEventsResponse

    suspend fun getEvent(request: GetEventRequest): ProtoEvent

    suspend fun createEvent(request: CreateEventRequest): ProtoEvent

    suspend fun updateEvent(request: UpdateEventRequest): ProtoEvent

    suspend fun deleteEvent(request: DeleteEventRequest): DeleteEventResponse
}
