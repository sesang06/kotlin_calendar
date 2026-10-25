package com.kotlincalendar.client.di

import com.kotlincalendar.client.data.remote.ApiConfig
import com.kotlincalendar.client.data.remote.grpc.GrpcClientFactory
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single<HttpClientEngine> { Js.create() }
        single { ApiConfig() }
        single<GrpcClientFactory> { GrpcClientFactory { null } }
    }
