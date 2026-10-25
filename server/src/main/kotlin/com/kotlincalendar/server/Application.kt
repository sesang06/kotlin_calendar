package com.kotlincalendar.server

import com.kotlincalendar.server.plugins.authModule
import com.kotlincalendar.server.plugins.configureCors
import com.kotlincalendar.server.plugins.configureDatabase
import com.kotlincalendar.server.plugins.configureGrpc
import com.kotlincalendar.server.plugins.configureKoin
import com.kotlincalendar.server.plugins.configureMonitoring
import com.kotlincalendar.server.plugins.configureRouting
import com.kotlincalendar.server.plugins.configureStatusPages
import com.kotlincalendar.server.plugins.databaseModule
import com.kotlincalendar.server.plugins.eventModule
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    configureKoin(listOf(databaseModule(environment.config), authModule, eventModule))
    configureDatabase()
    configureMonitoring()
    configureCors()
    configureStatusPages()
    configureRouting()
    configureGrpc()
}
