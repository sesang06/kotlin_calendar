package com.kotlincalendar.server.plugins

import com.kotlincalendar.server.common.auth.TokenProvider
import com.kotlincalendar.server.common.grpc.AuthServerInterceptor
import com.kotlincalendar.server.event.handler.grpc.EventGrpcHandler
import com.kotlincalendar.server.user.handler.grpc.AuthGrpcHandler
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import org.koin.ktor.ext.getKoin

fun Application.configureGrpc() {
    val authGrpcHandler = getKoin().getOrNull<AuthGrpcHandler>()
    val eventGrpcHandler = getKoin().getOrNull<EventGrpcHandler>()
    val tokenProvider = getKoin().getOrNull<TokenProvider>()
    val grpcPort =
        environment.config
            .propertyOrNull("grpc.port")
            ?.getString()
            ?.toIntOrNull() ?: 50051

    val builder = NettyServerBuilder.forPort(grpcPort)
    if (authGrpcHandler != null && tokenProvider != null) {
        builder.addService(ServerInterceptors.intercept(authGrpcHandler, AuthServerInterceptor(tokenProvider)))
    }
    if (eventGrpcHandler != null && tokenProvider != null) {
        builder.addService(ServerInterceptors.intercept(eventGrpcHandler, AuthServerInterceptor(tokenProvider)))
    }
    val server: Server = builder.build()

    monitor.subscribe(ApplicationStarted) {
        server.start()
    }
    monitor.subscribe(ApplicationStopping) {
        server.shutdown()
    }
}
