package com.kotlincalendar.server.event.handler.http

import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import com.kotlincalendar.server.common.http.receiveProto
import com.kotlincalendar.server.common.http.respondProto
import com.kotlincalendar.server.event.handler.parseDate
import com.kotlincalendar.server.event.handler.parseTimeZone
import com.kotlincalendar.server.event.handler.requireId
import com.kotlincalendar.server.event.handler.toCommand
import com.kotlincalendar.server.event.handler.toProto
import com.kotlincalendar.server.event.service.EventService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

class EventHttpHandler(
    private val eventService: EventService,
) {
    suspend fun list(call: ApplicationCall) {
        val date = parseDate(call.request.queryParameters["date"])
        val timeZone = parseTimeZone(call.request.queryParameters["time_zone"])
        val events = eventService.getEventsOn(date, timeZone)
        val response =
            ListEventsResponse
                .newBuilder()
                .addAllEvents(events.map { it.toProto() })
                .build()
        call.respondProto(response)
    }

    suspend fun get(call: ApplicationCall) {
        val id = requireId(call.parameters["id"])
        val event = eventService.getEvent(id)
        call.respondProto(event.toProto())
    }

    suspend fun create(call: ApplicationCall) {
        val request = call.receiveProto(CreateEventRequest.parser())
        val created = eventService.createEvent(request.toCommand())
        call.respondProto(created.toProto(), HttpStatusCode.Created)
    }

    suspend fun delete(call: ApplicationCall) {
        val id = requireId(call.parameters["id"])
        eventService.deleteEvent(id)
        call.respond(HttpStatusCode.NoContent)
    }
}
