package com.kotlincalendar.server.event.handler.http

import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import com.kotlincalendar.proto.calendar.v1.UpdateEventRequest
import com.kotlincalendar.server.common.auth.TokenProvider
import com.kotlincalendar.server.common.error.UnauthenticatedException
import com.kotlincalendar.server.common.http.receiveProto
import com.kotlincalendar.server.common.http.respondProto
import com.kotlincalendar.server.event.handler.parseDate
import com.kotlincalendar.server.event.handler.parseTimeZone
import com.kotlincalendar.server.event.handler.requireId
import com.kotlincalendar.server.event.handler.toCommand
import com.kotlincalendar.server.event.handler.toProto
import com.kotlincalendar.server.event.service.EventService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlin.time.Clock

class EventHttpHandler(
    private val eventService: EventService,
    private val tokenProvider: TokenProvider,
    private val clock: Clock = Clock.System,
) {
    private fun authenticate(call: ApplicationCall): String {
        val authHeader =
            call.request.headers[HttpHeaders.Authorization]
                ?: throw UnauthenticatedException("missing authorization header")
        if (!authHeader.startsWith("Bearer ", ignoreCase = true)) {
            throw UnauthenticatedException("invalid authorization scheme")
        }
        val token = authHeader.substringAfter("Bearer ").trim()
        return tokenProvider.verifyAccessToken(token, clock.now())
            ?: throw UnauthenticatedException("invalid or expired access token")
    }

    suspend fun list(call: ApplicationCall) {
        val userId = authenticate(call)
        val date = parseDate(call.request.queryParameters["date"])
        val timeZone = parseTimeZone(call.request.queryParameters["time_zone"])
        val events = eventService.getEventsOn(userId, date, timeZone)
        val response =
            ListEventsResponse
                .newBuilder()
                .addAllEvents(events.map { it.toProto() })
                .build()
        call.respondProto(response)
    }

    suspend fun listInRange(call: ApplicationCall) {
        val userId = authenticate(call)
        val params = call.request.queryParameters
        val events =
            eventService.getEventsInRange(
                userId = userId,
                startDate = parseDate(params["start_date"], "start_date"),
                endDate = parseDate(params["end_date"], "end_date"),
                timeZone = parseTimeZone(params["time_zone"]),
            )
        call.respondProto(ListEventsResponse.newBuilder().addAllEvents(events.map { it.toProto() }).build())
    }

    suspend fun get(call: ApplicationCall) {
        val userId = authenticate(call)
        val id = requireId(call.parameters["id"])
        val event = eventService.getEvent(userId, id)
        call.respondProto(event.toProto())
    }

    suspend fun create(call: ApplicationCall) {
        val userId = authenticate(call)
        val request = call.receiveProto(CreateEventRequest.parser())
        val created = eventService.createEvent(request.toCommand(ownerUserId = userId))
        call.respondProto(created.toProto(), HttpStatusCode.Created)
    }

    suspend fun update(call: ApplicationCall) {
        val userId = authenticate(call)
        val id = requireId(call.parameters["id"])
        val request = call.receiveProto(UpdateEventRequest.parser())
        val updated = eventService.updateEvent(request.toCommand(id = id, ownerUserId = userId))
        call.respondProto(updated.toProto())
    }

    suspend fun delete(call: ApplicationCall) {
        val userId = authenticate(call)
        val id = requireId(call.parameters["id"])
        eventService.deleteEvent(userId, id)
        call.respond(HttpStatusCode.NoContent)
    }
}
