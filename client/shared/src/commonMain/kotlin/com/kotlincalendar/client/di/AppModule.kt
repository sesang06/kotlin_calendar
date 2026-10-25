package com.kotlincalendar.client.di

import com.kotlincalendar.client.data.remote.ApiConfig
import com.kotlincalendar.client.data.remote.EventRemoteDataSource
import com.kotlincalendar.client.data.remote.Transport
import com.kotlincalendar.client.data.remote.grpc.GrpcClientFactory
import com.kotlincalendar.client.data.remote.grpc.GrpcEventRemoteDataSource
import com.kotlincalendar.client.data.remote.http.HttpEventRemoteDataSource
import com.kotlincalendar.client.data.remote.http.createHttpClient
import com.kotlincalendar.client.data.repository.EventRepositoryImpl
import com.kotlincalendar.client.domain.repository.EventRepository
import com.kotlincalendar.proto.calendar.v1.GrpcEventApiClient
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule =
    module {
        single { createHttpClient(get(), get()) }
        single<EventRemoteDataSource> {
            val config = get<ApiConfig>()
            val grpcClient =
                if (config.transport == Transport.GRPC) {
                    get<GrpcClientFactory>().create(config.grpcBaseUrl)
                } else {
                    null
                }
            grpcClient?.let { GrpcEventRemoteDataSource(GrpcEventApiClient(it)) }
                ?: HttpEventRemoteDataSource(get())
        }
        single<EventRepository> { EventRepositoryImpl(get()) }
    }

fun initKoin(extra: KoinAppDeclaration = {}) =
    startKoin {
        extra()
        modules(appModule, platformModule)
    }
