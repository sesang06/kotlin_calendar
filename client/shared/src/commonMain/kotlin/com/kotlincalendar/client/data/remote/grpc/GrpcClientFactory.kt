package com.kotlincalendar.client.data.remote.grpc

import com.squareup.wire.GrpcClient

fun interface GrpcClientFactory {
    fun create(baseUrl: String): GrpcClient?
}
