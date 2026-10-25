package com.kotlincalendar.server.event.handler.grpc

import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventResponse
import com.kotlincalendar.proto.calendar.v1.EventApiGrpcKt
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import com.kotlincalendar.server.common.grpc.grpcCall
import com.kotlincalendar.server.event.handler.parseDate
import com.kotlincalendar.server.event.handler.parseTimeZone
import com.kotlincalendar.server.event.handler.requireId
import com.kotlincalendar.server.event.handler.toCommand
import com.kotlincalendar.server.event.handler.toProto
import com.kotlincalendar.server.event.service.EventService
import com.kotlincalendar.proto.calendar.v1.Event as EventMessage

class EventGrpcHandler(
    private val eventService: EventService,
) : EventApiGrpcKt.EventApiCoroutineImplBase() {
    override suspend fun listEvents(request: ListEventsRequest): ListEventsResponse =
        grpcCall {
            val date = parseDate(request.date)
            val timeZone = parseTimeZone(request.timeZone)
            val events = eventService.getEventsOn(date, timeZone)
            ListEventsResponse
                .newBuilder()
                .addAllEvents(events.map { it.toProto() })
                .build()
        }

    override suspend fun getEvent(request: GetEventRequest): EventMessage =
        grpcCall {
            val event = eventService.getEvent(requireId(request.id))
            event.toProto()
        }

    override suspend fun createEvent(request: CreateEventRequest): EventMessage =
        grpcCall {
            val created = eventService.createEvent(request.toCommand())
            created.toProto()
        }

    override suspend fun deleteEvent(request: DeleteEventRequest): DeleteEventResponse =
        grpcCall {
            eventService.deleteEvent(requireId(request.id))
            DeleteEventResponse.getDefaultInstance()
        }
}
