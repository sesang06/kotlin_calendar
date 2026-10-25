package com.kotlincalendar.server.common.error

import io.grpc.Status
import io.ktor.http.HttpStatusCode

sealed class DomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    abstract fun toHttpStatus(): HttpStatusCode

    abstract fun toGrpcStatus(): Status
}

class ValidationException(
    message: String,
) : DomainException(message) {
    override fun toHttpStatus(): HttpStatusCode = HttpStatusCode.BadRequest

    override fun toGrpcStatus(): Status = Status.INVALID_ARGUMENT.withDescription(message)
}

class NotFoundException(
    message: String,
) : DomainException(message) {
    override fun toHttpStatus(): HttpStatusCode = HttpStatusCode.NotFound

    override fun toGrpcStatus(): Status = Status.NOT_FOUND.withDescription(message)
}

class ConflictException(
    message: String,
) : DomainException(message) {
    override fun toHttpStatus(): HttpStatusCode = HttpStatusCode.Conflict

    override fun toGrpcStatus(): Status = Status.ALREADY_EXISTS.withDescription(message)
}

class UnauthenticatedException(
    message: String = "unauthenticated",
) : DomainException(message) {
    override fun toHttpStatus(): HttpStatusCode = HttpStatusCode.Unauthorized

    override fun toGrpcStatus(): Status = Status.UNAUTHENTICATED.withDescription(message)
}
