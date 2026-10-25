package com.kotlincalendar.client.di

import com.kotlincalendar.client.data.local.BrowserKeyValueStorage
import com.kotlincalendar.client.data.local.KeyValueStorage
import com.kotlincalendar.client.data.remote.ApiConfig
import com.kotlincalendar.client.data.remote.Transport
import com.kotlincalendar.client.data.remote.grpc.GrpcClientFactory
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import kotlinx.browser.window
import org.koin.core.module.Module
import org.koin.dsl.module

// 로컬 dev server(:8081)에서는 Ktor 서버(:8080)로 직접, 배포(Vercel)에서는 같은 origin의 /api 프록시로 보낸다.
private const val DEV_SERVER_PORT = "8081"
private const val LOCAL_HTTP_BASE_URL = "http://localhost:8080"

// 시간대 DB가 없으면 kotlinx-datetime의 TimeZone.currentSystemDefault()가 IANA ID 대신 "SYSTEM"을 돌려주고,
// 서버는 이를 invalid time_zone(400)으로 거절한다. 모듈을 로드해 브라우저 시간대를 "Asia/Seoul" 같은 ID로 얻는다.
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule : JsAny

private val jsJodaTz = JsJodaTimeZoneModule

actual val platformModule: Module =
    module {
        single<KeyValueStorage> { BrowserKeyValueStorage() }
        single<HttpClientEngine> { Js.create() }
        single {
            val location = window.location
            ApiConfig(
                httpBaseUrl = if (location.port == DEV_SERVER_PORT) LOCAL_HTTP_BASE_URL else location.origin,
                grpcBaseUrl = "",
                transport = Transport.HTTP,
            )
        }
        single<GrpcClientFactory> { GrpcClientFactory { null } }
    }
