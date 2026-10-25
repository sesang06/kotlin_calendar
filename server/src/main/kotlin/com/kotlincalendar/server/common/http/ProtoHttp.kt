package com.kotlincalendar.server.common.http

import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import com.kotlincalendar.server.common.error.ValidationException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respondBytes

val ProtobufContentType = ContentType("application", "x-protobuf")

suspend fun <T : MessageLite> ApplicationCall.receiveProto(parser: Parser<T>): T {
    val bytes = receive<ByteArray>()
    return runCatching { parser.parseFrom(bytes) }.getOrElse {
        throw ValidationException("Invalid protobuf payload: ${it.message}")
    }
}

suspend fun ApplicationCall.respondProto(
    message: MessageLite,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondBytes(
        bytes = message.toByteArray(),
        contentType = ProtobufContentType,
        status = status,
    )
}
