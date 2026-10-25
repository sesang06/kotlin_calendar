package com.kotlincalendar.client.di

import com.kotlincalendar.client.data.remote.ApiConfig
import com.kotlincalendar.client.data.remote.grpc.GrpcClientFactory
import com.squareup.wire.GrpcClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single<HttpClientEngine> { OkHttp.create() }
        single { ApiConfig(httpBaseUrl = "http://localhost:8080", grpcBaseUrl = "http://localhost:50051") }
        single<GrpcClientFactory> {
            GrpcClientFactory { baseUrl ->
                val okHttpClient =
                    OkHttpClient
                        .Builder()
                        .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
                        .build()
                GrpcClient
                    .Builder()
                    .client(okHttpClient)
                    .baseUrl(baseUrl)
                    .build()
            }
        }
    }
