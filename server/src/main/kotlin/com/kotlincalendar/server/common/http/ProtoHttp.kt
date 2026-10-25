package com.kotlincalendar.server.common.http

import com.google.protobuf.Message
import com.google.protobuf.Parser
import com.kotlincalendar.server.common.error.ValidationException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.utils.io.toByteArray

val ProtobufContentType = ContentType("application", "x-protobuf")

suspend fun <T : Message> ApplicationCall.receiveProto(parser: Parser<T>): T =
    try {
        val bytes = receiveChannel().toByteArray()
        parser.parseFrom(bytes)
    } catch (e: Exception) {
        throw ValidationException("Invalid protobuf payload: ${e.message}")
    }

suspend fun ApplicationCall.respondProto(
    message: Message,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val bytes = message.toByteArray()
    response.header("Content-Type", ProtobufContentType.toString())
    respondBytes(
        bytes = bytes,
        contentType = ProtobufContentType,
        status = status,
    )
}
