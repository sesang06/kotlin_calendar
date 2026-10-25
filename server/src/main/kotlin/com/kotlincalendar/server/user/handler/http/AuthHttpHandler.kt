package com.kotlincalendar.server.user.handler.http

import com.kotlincalendar.proto.auth.v1.LoginRequest
import com.kotlincalendar.proto.auth.v1.LogoutRequest
import com.kotlincalendar.proto.auth.v1.RefreshTokenRequest
import com.kotlincalendar.proto.auth.v1.SignUpRequest
import com.kotlincalendar.server.common.auth.TokenProvider
import com.kotlincalendar.server.common.error.UnauthenticatedException
import com.kotlincalendar.server.common.http.receiveProto
import com.kotlincalendar.server.common.http.respondProto
import com.kotlincalendar.server.user.handler.toCommand
import com.kotlincalendar.server.user.handler.toLoginResponse
import com.kotlincalendar.server.user.handler.toRefreshTokenResponse
import com.kotlincalendar.server.user.handler.toSignUpResponse
import com.kotlincalendar.server.user.service.UserService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlin.time.Clock

class AuthHttpHandler(
    private val userService: UserService,
    private val tokenProvider: TokenProvider,
    private val clock: Clock = Clock.System,
) {
    suspend fun signUp(call: ApplicationCall) {
        val request = call.receiveProto(SignUpRequest.parser())
        val result = userService.signUp(request.toCommand())
        call.respondProto(result.toSignUpResponse(), HttpStatusCode.Created)
    }

    suspend fun login(call: ApplicationCall) {
        val request = call.receiveProto(LoginRequest.parser())
        val result = userService.login(request.toCommand())
        call.respondProto(result.toLoginResponse(), HttpStatusCode.OK)
    }

    suspend fun refreshToken(call: ApplicationCall) {
        val request = call.receiveProto(RefreshTokenRequest.parser())
        val tokens = userService.refreshToken(request.toCommand())
        call.respondProto(tokens.toRefreshTokenResponse(), HttpStatusCode.OK)
    }

    suspend fun logout(call: ApplicationCall) {
        val authHeader =
            call.request.headers[HttpHeaders.Authorization]
                ?: throw UnauthenticatedException("missing authorization header")
        if (!authHeader.startsWith("Bearer ", ignoreCase = true)) {
            throw UnauthenticatedException("invalid authorization scheme")
        }
        val accessToken = authHeader.substringAfter("Bearer ").trim()
        val userId =
            tokenProvider.verifyAccessToken(accessToken, clock.now())
                ?: throw UnauthenticatedException("invalid or expired access token")

        val request = call.receiveProto(LogoutRequest.parser())
        userService.logout(request.toCommand(userId))
        call.respond(HttpStatusCode.NoContent)
    }
}
