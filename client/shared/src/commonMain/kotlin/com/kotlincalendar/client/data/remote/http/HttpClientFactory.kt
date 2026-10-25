package com.kotlincalendar.client.data.remote.http

import com.kotlincalendar.client.data.remote.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging

fun createHttpClient(
    engine: HttpClientEngine,
    config: ApiConfig,
): HttpClient =
    HttpClient(engine) {
        install(Logging) {
            level = LogLevel.INFO
        }
        defaultRequest {
            url(config.httpBaseUrl)
        }
        expectSuccess = true
    }
