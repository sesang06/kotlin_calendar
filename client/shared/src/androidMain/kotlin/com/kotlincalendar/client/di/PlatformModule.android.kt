package com.kotlincalendar.client.di

import com.kotlincalendar.client.data.remote.ApiConfig
import com.kotlincalendar.client.data.remote.grpc.GrpcClientFactory
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single<HttpClientEngine> { OkHttp.create() }
        single {
            ApiConfig(
                httpBaseUrl = "http://10.0.2.2:8080",
                grpcBaseUrl = "http://10.0.2.2:50051",
            )
        }
        single<GrpcClientFactory> { GrpcClientFactory { null } }
    }
