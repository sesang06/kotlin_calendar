package com.kotlincalendar.client.di

import com.kotlincalendar.client.data.local.KeyValueStorage
import com.kotlincalendar.client.data.local.UserDefaultsKeyValueStorage
import com.kotlincalendar.client.data.remote.ApiConfig
import com.kotlincalendar.client.data.remote.grpc.GrpcClientFactory
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single<KeyValueStorage> { UserDefaultsKeyValueStorage() }
        single<HttpClientEngine> { Darwin.create() }
        single { ApiConfig(httpBaseUrl = "http://localhost:8080", grpcBaseUrl = "http://localhost:50051") }
        single<GrpcClientFactory> { GrpcClientFactory { null } }
    }
