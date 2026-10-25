package com.kotlincalendar.server.common.grpc

import com.kotlincalendar.server.common.error.DomainException
import io.grpc.Status
import io.grpc.StatusException

inline fun <T> grpcCall(block: () -> T): T =
    try {
        block()
    } catch (e: DomainException) {
        throw e.toGrpcStatus().asException()
    } catch (e: StatusException) {
        throw e
    } catch (e: Throwable) {
        throw Status.INTERNAL
            .withDescription(e.message ?: "internal server error")
            .withCause(e)
            .asException()
    }
