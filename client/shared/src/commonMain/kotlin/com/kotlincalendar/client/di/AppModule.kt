package com.kotlincalendar.client.di

import com.kotlincalendar.client.data.remote.ApiConfig
import com.kotlincalendar.client.data.remote.AuthRemoteDataSource
import com.kotlincalendar.client.data.remote.Transport
import com.kotlincalendar.client.data.remote.grpc.GrpcAuthRemoteDataSource
import com.kotlincalendar.client.data.remote.grpc.GrpcClientFactory
import com.kotlincalendar.client.data.remote.http.HttpAuthRemoteDataSource
import com.kotlincalendar.client.data.repository.AuthRepositoryImpl
import com.kotlincalendar.client.data.repository.InMemoryTokenStorage
import com.kotlincalendar.client.domain.repository.AuthRepository
import com.kotlincalendar.client.domain.repository.TokenStorage
import com.kotlincalendar.client.presentation.auth.login.LoginViewModel
import com.kotlincalendar.client.presentation.auth.signup.SignUpViewModel
import com.kotlincalendar.proto.auth.v1.GrpcAuthApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun createHttpClient(
    engine: HttpClientEngine,
    config: ApiConfig,
): HttpClient =
    HttpClient(engine) {
        install(Logging) { level = LogLevel.INFO }
        defaultRequest { url(config.httpBaseUrl) }
        expectSuccess = true
    }

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
        viewModelOf(::LoginViewModel)
        viewModelOf(::SignUpViewModel)
    }

fun initKoin(extra: KoinAppDeclaration = {}) =
    startKoin {
        extra()
        modules(appModule, platformModule)
    }
