package com.kotlincalendar.server.user.handler.http

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(handler: AuthHttpHandler) {
    route("/auth") {
        post("/signup") { handler.signUp(call) }
        post("/login") { handler.login(call) }
        post("/refresh") { handler.refreshToken(call) }
        post("/logout") { handler.logout(call) }
        get("/me") { handler.me(call) }
    }
}
