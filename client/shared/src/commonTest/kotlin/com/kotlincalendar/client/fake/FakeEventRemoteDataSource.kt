package com.kotlincalendar.client.fake

import com.kotlincalendar.client.data.remote.EventRemoteDataSource
import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventResponse
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import com.kotlincalendar.proto.calendar.v1.UpdateEventRequest
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

class FakeEventRemoteDataSource : EventRemoteDataSource {
    var listEventsResponse: ListEventsResponse = ListEventsResponse()
    var getEventResponse: ProtoEvent = ProtoEvent()
    var createEventResponse: ProtoEvent = ProtoEvent()
    var updateEventResponse: ProtoEvent = ProtoEvent()
    var deleteEventResponse: DeleteEventResponse = DeleteEventResponse()

    val receivedListRequests = mutableListOf<ListEventsRequest>()
    val receivedGetRequests = mutableListOf<GetEventRequest>()
    val receivedCreateRequests = mutableListOf<CreateEventRequest>()
    val receivedUpdateRequests = mutableListOf<UpdateEventRequest>()
    val receivedDeleteRequests = mutableListOf<DeleteEventRequest>()

    var shouldThrow: Throwable? = null

    override suspend fun listEvents(request: ListEventsRequest): ListEventsResponse {
        shouldThrow?.let { throw it }
        receivedListRequests += request
        return listEventsResponse
    }

    override suspend fun getEvent(request: GetEventRequest): ProtoEvent {
        shouldThrow?.let { throw it }
        receivedGetRequests += request
        return getEventResponse
    }

    override suspend fun createEvent(request: CreateEventRequest): ProtoEvent {
        shouldThrow?.let { throw it }
        receivedCreateRequests += request
        return createEventResponse
    }

    override suspend fun updateEvent(request: UpdateEventRequest): ProtoEvent {
        shouldThrow?.let { throw it }
        receivedUpdateRequests += request
        return updateEventResponse
    }

    override suspend fun deleteEvent(request: DeleteEventRequest): DeleteEventResponse {
        shouldThrow?.let { throw it }
        receivedDeleteRequests += request
        return deleteEventResponse
    }
}
