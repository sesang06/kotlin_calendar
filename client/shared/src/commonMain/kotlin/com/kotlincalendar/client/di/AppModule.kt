package com.kotlincalendar.client.di

import com.kotlincalendar.client.data.remote.ApiConfig
import com.kotlincalendar.client.data.remote.AuthRemoteDataSource
import com.kotlincalendar.client.data.remote.EventRemoteDataSource
import com.kotlincalendar.client.data.remote.Transport
import com.kotlincalendar.client.data.remote.grpc.GrpcAuthRemoteDataSource
import com.kotlincalendar.client.data.remote.grpc.GrpcClientFactory
import com.kotlincalendar.client.data.remote.grpc.GrpcEventRemoteDataSource
import com.kotlincalendar.client.data.remote.http.HttpAuthRemoteDataSource
import com.kotlincalendar.client.data.remote.http.HttpEventRemoteDataSource
import com.kotlincalendar.client.data.remote.http.createHttpClient
import com.kotlincalendar.client.data.repository.AuthRepositoryImpl
import com.kotlincalendar.client.data.repository.EventRepositoryImpl
import com.kotlincalendar.client.data.repository.InMemoryTokenStorage
import com.kotlincalendar.client.domain.repository.AuthRepository
import com.kotlincalendar.client.domain.repository.EventRepository
import com.kotlincalendar.client.domain.repository.TokenStorage
import com.kotlincalendar.client.presentation.auth.login.LoginViewModel
import com.kotlincalendar.client.presentation.auth.signup.SignUpViewModel
import com.kotlincalendar.client.presentation.calendar.CalendarViewModel
import com.kotlincalendar.proto.auth.v1.GrpcAuthApiClient
import com.kotlincalendar.proto.calendar.v1.GrpcEventApiClient
import kotlinx.datetime.TimeZone
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.time.Clock

val appModule =
    module {
        single { createHttpClient(get(), get()) }
        single<TokenStorage> { InMemoryTokenStorage() }
        single<AuthRemoteDataSource> {
            val config = get<ApiConfig>()
            val grpcFactory = getOrNull<GrpcClientFactory>()
            val grpcClient =
                if (config.transport == Transport.GRPC) {
                    grpcFactory?.create(config.grpcBaseUrl)
                } else {
                    null
                }
            grpcClient?.let { GrpcAuthRemoteDataSource(GrpcAuthApiClient(it)) }
                ?: HttpAuthRemoteDataSource(get())
        }
        single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
        single<EventRemoteDataSource> {
            val config = get<ApiConfig>()
            val grpcFactory = getOrNull<GrpcClientFactory>()
            val grpcClient =
                if (config.transport == Transport.GRPC) {
                    grpcFactory?.create(config.grpcBaseUrl)
                } else {
                    null
                }
            grpcClient?.let { GrpcEventRemoteDataSource(GrpcEventApiClient(it)) }
                ?: HttpEventRemoteDataSource(get())
        }
        single<EventRepository> { EventRepositoryImpl(get()) }
        viewModelOf(::LoginViewModel)
        viewModelOf(::SignUpViewModel)
        single<Clock> { Clock.System }
        single<TimeZone> { TimeZone.currentSystemDefault() }
        viewModelOf(::CalendarViewModel)
    }

fun initKoin(extra: KoinAppDeclaration = {}) =
    startKoin {
        extra()
        modules(appModule, platformModule)
    }
