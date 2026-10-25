package com.kotlincalendar.client.data.remote

enum class Transport {
    HTTP,
    GRPC,
}

data class ApiConfig(
    val httpBaseUrl: String = "http://localhost:8080",
    val grpcBaseUrl: String = "http://localhost:50051",
    val transport: Transport = Transport.HTTP,
)
