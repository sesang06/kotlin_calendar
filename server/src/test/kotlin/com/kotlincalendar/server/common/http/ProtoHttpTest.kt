package com.kotlincalendar.server.common.http

import com.kotlincalendar.proto.calendar.v1.ErrorResponse
import com.kotlincalendar.server.common.error.NotFoundException
import com.kotlincalendar.server.common.error.ValidationException
import com.kotlincalendar.server.plugins.configureStatusPages
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ProtoHttpTest {
    @Test
    fun testReceiveAndRespondProto() =
        testApplication {
            application {
                routing {
                    post("/test/echo") {
                        val req = call.receiveProto(ErrorResponse.parser())
                        call.respondProto(req, HttpStatusCode.Created)
                    }
                }
            }

            val requestProto = ErrorResponse.newBuilder().setMessage("hello world").build()
            val response =
                client.post("/test/echo") {
                    header("Content-Type", ProtobufContentType.toString())
                    setBody(requestProto.toByteArray())
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("application/x-protobuf", response.headers["Content-Type"])

            val parsedResponse = ErrorResponse.parseFrom(response.bodyAsBytes())
            assertEquals("hello world", parsedResponse.message)
        }

    @Test
    fun testStatusPagesDomainException() =
        testApplication {
            application {
                configureStatusPages()
                routing {
                    get("/test/validation") {
                        throw ValidationException("invalid field")
                    }
                    get("/test/notfound") {
                        throw NotFoundException("item not found")
                    }
                }
            }

            val valRes = client.get("/test/validation")
            assertEquals(HttpStatusCode.BadRequest, valRes.status)
            val valProto = ErrorResponse.parseFrom(valRes.bodyAsBytes())
            assertEquals("invalid field", valProto.message)

            val nfRes = client.get("/test/notfound")
            assertEquals(HttpStatusCode.NotFound, nfRes.status)
            val nfProto = ErrorResponse.parseFrom(nfRes.bodyAsBytes())
            assertEquals("item not found", nfProto.message)
        }
}
