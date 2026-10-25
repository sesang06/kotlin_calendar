package com.kotlincalendar.server.common.grpc

import com.kotlincalendar.server.common.auth.TokenProvider
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import kotlin.time.Clock

val AUTH_USER_ID_KEY: Context.Key<String> = Context.key("auth_user_id")
val AUTHORIZATION_METADATA_KEY: Metadata.Key<String> =
    Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

class AuthServerInterceptor(
    private val tokenProvider: TokenProvider,
    private val clock: Clock = Clock.System,
) : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val authHeader = headers.get(AUTHORIZATION_METADATA_KEY)
        val context =
            if (authHeader != null && authHeader.startsWith("Bearer ", ignoreCase = true)) {
                val token = authHeader.substringAfter("Bearer ").trim()
                val userId = tokenProvider.verifyAccessToken(token, clock.now())
                if (userId != null) {
                    Context.current().withValue(AUTH_USER_ID_KEY, userId)
                } else {
                    Context.current()
                }
            } else {
                Context.current()
            }
        return Contexts.interceptCall(context, call, headers, next)
    }
}
