package com.kotlincalendar.server.plugins

import com.kotlincalendar.server.common.auth.HmacTokenProvider
import com.kotlincalendar.server.common.auth.PasswordHasher
import com.kotlincalendar.server.common.auth.Pbkdf2PasswordHasher
import com.kotlincalendar.server.common.auth.TokenProvider
import com.kotlincalendar.server.event.handler.grpc.EventGrpcHandler
import com.kotlincalendar.server.event.handler.http.EventHttpHandler
import com.kotlincalendar.server.event.repository.EventRepository
import com.kotlincalendar.server.event.repository.ExposedEventRepository
import com.kotlincalendar.server.event.service.EventService
import com.kotlincalendar.server.user.handler.grpc.AuthGrpcHandler
import com.kotlincalendar.server.user.handler.http.AuthHttpHandler
import com.kotlincalendar.server.user.repository.ExposedRefreshTokenRepository
import com.kotlincalendar.server.user.repository.ExposedUserRepository
import com.kotlincalendar.server.user.repository.RefreshTokenRepository
import com.kotlincalendar.server.user.repository.UserRepository
import com.kotlincalendar.server.user.service.UserService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

val authModule =
    module {
        singleOf(::ExposedUserRepository) bind UserRepository::class
        singleOf(::ExposedRefreshTokenRepository) bind RefreshTokenRepository::class
        single<PasswordHasher> { Pbkdf2PasswordHasher() }
        single<TokenProvider> { HmacTokenProvider() }
        single { UserService(get(), get(), get(), get()) }
        single { AuthHttpHandler(get(), get()) }
        singleOf(::AuthGrpcHandler)
    }

val eventModule =
    module {
        singleOf(::ExposedEventRepository) bind EventRepository::class
        single { EventService(get()) }
        singleOf(::EventHttpHandler)
        singleOf(::EventGrpcHandler)
    }

fun Application.configureKoin(modules: List<Module>) {
    install(Koin) {
        slf4jLogger()
        modules(modules)
    }
}
