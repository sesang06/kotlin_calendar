package com.kotlincalendar.server.common.http

import com.kotlincalendar.proto.calendar.v1.ErrorResponse
import com.kotlincalendar.server.common.error.DomainException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<DomainException> { call, cause ->
            val errorResponse =
                ErrorResponse
                    .newBuilder()
                    .setMessage(cause.message ?: "error")
                    .build()
            call.respondProto(errorResponse, cause.toHttpStatus())
        }
        exception<Throwable> { call, cause ->
            val errorResponse =
                ErrorResponse
                    .newBuilder()
                    .setMessage(cause.message ?: "internal server error")
                    .build()
            call.respondProto(errorResponse, io.ktor.http.HttpStatusCode.InternalServerError)
        }
    }
}
