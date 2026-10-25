package com.kotlincalendar.server.plugins

import com.kotlincalendar.server.event.handler.http.EventHttpHandler
import com.kotlincalendar.server.event.handler.http.eventRoutes
import com.kotlincalendar.server.user.handler.http.AuthHttpHandler
import com.kotlincalendar.server.user.handler.http.authRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
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
        // CORS는 Origin의 포트까지 비교하므로 host:port를 그대로 넘긴다.
        hosts
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { hostPort -> allowHost(hostPort, listOf("http", "https")) }
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
