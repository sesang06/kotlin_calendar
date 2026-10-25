package com.kotlincalendar.server.event.handler.http

import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.eventRoutes(handler: EventHttpHandler) {
    route("/events") {
        get { handler.list(call) }
        post { handler.create(call) }
        get("/{id}") { handler.get(call) }
        delete("/{id}") { handler.delete(call) }
    }
}
