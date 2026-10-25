package com.kotlincalendar.client.data.remote.auth

import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode

/** HTTP 401 또는 gRPC UNAUTHENTICATED로 실패했는지. */
fun Throwable.isUnauthenticated(): Boolean =
    when (this) {
        is ResponseException -> response.status == HttpStatusCode.Unauthorized
        is GrpcException -> grpcStatus == GrpcStatus.UNAUTHENTICATED
        else -> false
    }
