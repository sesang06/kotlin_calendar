package com.kotlincalendar.client.fake

import com.kotlincalendar.proto.calendar.v1.CreateEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventRequest
import com.kotlincalendar.proto.calendar.v1.DeleteEventResponse
import com.kotlincalendar.proto.calendar.v1.EventApiClient
import com.kotlincalendar.proto.calendar.v1.GetEventRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsRequest
import com.kotlincalendar.proto.calendar.v1.ListEventsResponse
import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcMethod
import okio.Timeout
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

/**
 * Wire 생성 [EventApiClient]의 Fake. 실행된 call의 request metadata를 [executedMetadata]에 기록하고,
 * [handler]가 응답(또는 예외)을 결정한다.
 */
class FakeEventApiClient : EventApiClient {
    val executedMetadata = mutableListOf<Map<String, String>>()
    var handler: (metadata: Map<String, String>, request: Any) -> Any = { _, request ->
        when (request) {
            is ListEventsRequest -> ListEventsResponse()
            is DeleteEventRequest -> DeleteEventResponse()
            else -> ProtoEvent()
        }
    }

    override fun ListEvents(): GrpcCall<ListEventsRequest, ListEventsResponse> = FakeGrpcCall()

    override fun GetEvent(): GrpcCall<GetEventRequest, ProtoEvent> = FakeGrpcCall()

    override fun CreateEvent(): GrpcCall<CreateEventRequest, ProtoEvent> = FakeGrpcCall()

    override fun DeleteEvent(): GrpcCall<DeleteEventRequest, DeleteEventResponse> = FakeGrpcCall()

    private inner class FakeGrpcCall<S : Any, R : Any> : GrpcCall<S, R> {
        private var executed = false

        override val method: GrpcMethod<S, R> get() = throw NotImplementedError()
        override val timeout: Timeout get() = throw NotImplementedError()
        override var requestMetadata: Map<String, String> = emptyMap()
        override val responseMetadata: Map<String, String>? = null

        override fun cancel() = Unit

        override fun isCanceled(): Boolean = false

        @Suppress("UNCHECKED_CAST")
        override suspend fun execute(request: S): R {
            check(!executed) { "GrpcCall can be executed only once" }
            executed = true
            executedMetadata += requestMetadata
            return handler(requestMetadata, request) as R
        }

        override fun executeBlocking(request: S): R = throw NotImplementedError()

        override fun enqueue(
            request: S,
            callback: GrpcCall.Callback<S, R>,
        ) = throw NotImplementedError()

        override fun isExecuted(): Boolean = executed

        override fun clone(): GrpcCall<S, R> = FakeGrpcCall()
    }
}
