package com.kotlincalendar.server.plugins

import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CorsTest {
    private fun ApplicationTestBuilder.setUpCors() {
        environment {
            config = MapApplicationConfig("cors.hosts" to "localhost:8081, localhost:8080")
        }
        application {
            configureCors()
            routing { post("/api/auth/signup") { } }
        }
    }

    @Test
    fun `preflight from allowed host and port succeeds`() =
        testApplication {
            setUpCors()

            val response =
                client.options("/api/auth/signup") {
                    header(HttpHeaders.Origin, "http://localhost:8081")
                    header(HttpHeaders.AccessControlRequestMethod, "POST")
                    header(HttpHeaders.AccessControlRequestHeaders, "content-type")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("http://localhost:8081", response.headers[HttpHeaders.AccessControlAllowOrigin])
        }

    @Test
    fun `preflight from unlisted port is forbidden`() =
        testApplication {
            setUpCors()

            val response =
                client.options("/api/auth/signup") {
                    header(HttpHeaders.Origin, "http://localhost:9999")
                    header(HttpHeaders.AccessControlRequestMethod, "POST")
                }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
}
