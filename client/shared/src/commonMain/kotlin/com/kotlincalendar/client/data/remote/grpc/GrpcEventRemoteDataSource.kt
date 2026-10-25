package com.kotlincalendar.client.data.remote.grpc

import com.kotlincalendar.client.data.remote.EventRemoteDataSource
import com.kotlincalendar.client.data.remote.auth.AuthSession
import com.kotlincalendar.client.data.remote.auth.withAuthRetry
import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventResponse
import com.kotlincalendar.proto.calendar.v1.EventApiClient
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import com.squareup.wire.GrpcCall
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

/** GrpcCall은 한 번만 실행할 수 있으므로 재시도할 때마다 새 call을 만든다. */
class GrpcEventRemoteDataSource(
    private val api: EventApiClient,
    private val authSession: AuthSession,
) : EventRemoteDataSource {
    override suspend fun listEvents(request: ListEventsRequest): ListEventsResponse =
        authSession.withAuthRetry { token -> api.ListEvents().withBearer(token).execute(request) }

    override suspend fun getEvent(request: GetEventRequest): ProtoEvent =
        authSession.withAuthRetry { token -> api.GetEvent().withBearer(token).execute(request) }

    override suspend fun createEvent(request: CreateEventRequest): ProtoEvent =
        authSession.withAuthRetry { token -> api.CreateEvent().withBearer(token).execute(request) }

    override suspend fun deleteEvent(request: DeleteEventRequest): DeleteEventResponse =
        authSession.withAuthRetry { token -> api.DeleteEvent().withBearer(token).execute(request) }
}

internal fun <S : Any, R : Any> GrpcCall<S, R>.withBearer(token: String?): GrpcCall<S, R> =
    apply {
        if (token != null) {
            requestMetadata = requestMetadata + ("authorization" to "Bearer $token")
        }
    }
