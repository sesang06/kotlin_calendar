package com.kotlincalendar.server.plugins

import com.kotlincalendar.proto.calendar.v1.ErrorResponse
import com.kotlincalendar.server.common.error.DomainException
import com.kotlincalendar.server.common.http.respondProto
import com.kotlincalendar.server.event.handler.http.EventHttpHandler
import com.kotlincalendar.server.event.handler.http.eventRoutes
import com.kotlincalendar.server.user.handler.http.AuthHttpHandler
import com.kotlincalendar.server.user.handler.http.authRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.getKoin

fun Application.configureMonitoring() {
    install(CallLogging)
}

fun Application.configureCors() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowNonSimpleContentTypes = true

        val hosts =
            this@configureCors
                .environment.config
                .propertyOrNull("cors.hosts")
                ?.getString()
                ?.split(",") ?: emptyList()
        hosts.forEach { hostPort ->
            val trimmed = hostPort.trim()
            if (trimmed.isNotEmpty()) {
                val parts = trimmed.split(":")
                val host = parts[0]
                allowHost(host, listOf("http", "https"))
            }
        }
        anyHost()
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<DomainException> { call, cause ->
            call.respondProto(cause.toErrorResponse(), cause.httpStatusCode)
        }
        exception<Throwable> { call, cause ->
            val errorResponse =
                ErrorResponse
                    .newBuilder()
                    .setMessage(cause.message ?: "Internal server error")
                    .build()
            call.respondProto(errorResponse, HttpStatusCode.InternalServerError)
        }
    }
}

fun Application.configureRouting() {
    val authHttpHandler = getKoin().getOrNull<AuthHttpHandler>()
    val eventHttpHandler = getKoin().getOrNull<EventHttpHandler>()
    routing {
        route("/api") {
            authHttpHandler?.let { authRoutes(it) }
            eventHttpHandler?.let { eventRoutes(it) }
        }
    }
}
