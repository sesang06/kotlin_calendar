package com.kotlincalendar.server.common.error

import com.kotlincalendar.proto.calendar.v1.ErrorResponse
import io.grpc.Status
import io.ktor.http.HttpStatusCode

sealed class DomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    abstract val httpStatusCode: HttpStatusCode
    abstract val grpcStatus: Status

    open fun toHttpStatus(): HttpStatusCode = httpStatusCode

    open fun toGrpcStatus(): Status = grpcStatus

    fun toErrorResponse(): ErrorResponse =
        ErrorResponse
            .newBuilder()
            .setMessage(message ?: "")
            .build()
}

class ValidationException(
    message: String,
) : DomainException(message) {
    override val httpStatusCode: HttpStatusCode = HttpStatusCode.BadRequest
    override val grpcStatus: Status = Status.INVALID_ARGUMENT.withDescription(message)
}

class NotFoundException(
    message: String,
) : DomainException(message) {
    override val httpStatusCode: HttpStatusCode = HttpStatusCode.NotFound
    override val grpcStatus: Status = Status.NOT_FOUND.withDescription(message)
}

class ConflictException(
    message: String,
) : DomainException(message) {
    override val httpStatusCode: HttpStatusCode = HttpStatusCode.Conflict
    override val grpcStatus: Status = Status.ALREADY_EXISTS.withDescription(message)
}

class UnauthenticatedException(
    message: String = "unauthenticated",
) : DomainException(message) {
    override val httpStatusCode: HttpStatusCode = HttpStatusCode.Unauthorized
    override val grpcStatus: Status = Status.UNAUTHENTICATED.withDescription(message)
}
