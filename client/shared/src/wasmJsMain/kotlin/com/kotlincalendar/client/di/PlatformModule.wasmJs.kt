package com.kotlincalendar.client.di

import com.kotlincalendar.client.data.remote.ApiConfig
import com.kotlincalendar.client.data.remote.Transport
import com.kotlincalendar.client.data.remote.grpc.GrpcClientFactory
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import kotlinx.browser.window
import org.koin.core.module.Module
import org.koin.dsl.module

// 로컬 dev server(:8081)에서는 Ktor 서버(:8080)로 직접, 배포(Vercel)에서는 같은 origin의 /api 프록시로 보낸다.
private const val DEV_SERVER_PORT = "8081"
private const val LOCAL_HTTP_BASE_URL = "http://localhost:8080"

actual val platformModule: Module =
    module {
        single<HttpClientEngine> { Js.create() }
        single {
            val location = window.location
            ApiConfig(
                httpBaseUrl = if (location.port == DEV_SERVER_PORT) LOCAL_HTTP_BASE_URL else location.origin,
                grpcBaseUrl = "",
                transport = Transport.HTTP,
            )
        }
        single<GrpcClientFactory> { GrpcClientFactory { null } }
    }
