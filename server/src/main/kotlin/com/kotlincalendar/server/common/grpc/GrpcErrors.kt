package com.kotlincalendar.server.common.grpc

import com.kotlincalendar.server.common.error.DomainException
import io.grpc.Status
import io.grpc.StatusException

suspend fun <T> grpcCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: DomainException) {
        throw e.grpcStatus.asException()
    } catch (e: StatusException) {
        throw e
    } catch (e: Exception) {
        throw Status.INTERNAL
            .withDescription(e.message)
            .withCause(e)
            .asException()
    }
