---
name: ktor-handler
description: kotlin_calendar 서버의 Handler 계층 규칙. proto 메시지를 받아 도메인으로 바꾸고 Service를 호출하는 HTTP Handler(Ktor 라우트)와 gRPC Handler(grpc-kotlin ImplBase) 작성, proto↔도메인 매퍼, 요청 검증, HTTP 상태 코드 결정을 다룰 때 사용. 새 API 엔드포인트·RPC를 추가하거나 고칠 때 사용.
---

# Handler 계층

Handler는 **전송 계층(HTTP/gRPC)과 도메인 사이의 번역기**다. 비즈니스 판단을 하지 않는다. 전송 방식별 공통 규칙은 `api-transport`.

## 책임

- 할 일: proto 요청 파싱, 경로/쿼리 파라미터 추출, 형식 검증, proto ↔ 도메인 매핑, Service 호출, 응답 proto와 HTTP 상태 결정
- 하지 않을 일: 비즈니스 규칙, Repository 직접 호출, 도메인 예외를 try/catch해서 상태 코드 만들기 (HTTP는 StatusPages, gRPC는 `grpcCall`)

## 구조

```
<feature>/handler/
├── EventProtoMapper.kt     # Event.toProto(), CreateEventRequest.toCommand(), parseDate(), requireId()
├── http/
│   ├── EventRoutes.kt      # Route 확장 함수: URL ↔ Handler 메서드 연결만
│   └── EventHttpHandler.kt
└── grpc/
    └── EventGrpcHandler.kt # EventApiGrpcKt.EventApiCoroutineImplBase 상속
```

## 매퍼

```kotlin
import com.google.protobuf.Timestamp
import com.kotlincalendar.proto.calendar.v1.Event as EventMessage   // 도메인 Event와 이름 충돌 → alias

fun Instant.toTimestamp(): Timestamp =
    Timestamp.newBuilder()
        .setSeconds(epochSeconds)
        .setNanos(nanosecondsOfSecond)
        .build()

fun Timestamp.toInstant(): Instant =
    Instant.fromEpochSeconds(seconds, nanos)

fun Event.toProto(): EventMessage =
    EventMessage.newBuilder()
        .setId(id)
        .setTitle(title)
        .setStartTime(start.toTimestamp())
        .setEndTime(end.toTimestamp())
        .setCreatedAt(createdAt.toTimestamp())
        .build()

fun CreateEventRequest.toCommand() =
    CreateEventCommand(title, startTime.toInstant(), endTime.toInstant())

fun parseDate(raw: String): LocalDate =
    runCatching { LocalDate.parse(raw) }.getOrElse { throw ValidationException("invalid date '$raw' (expected yyyy-MM-dd)") }

fun requireId(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: throw ValidationException("id is required")
```

형식 오류는 `ValidationException`으로 던진다 → HTTP 400 / gRPC `INVALID_ARGUMENT`로 두 전송 방식이 같은 결과.

## HTTP Handler

```kotlin
fun Route.eventRoutes(handler: EventHttpHandler) {
    route("/events") {
        get { handler.list(call) }
        post { handler.create(call) }
        get("/{id}") { handler.get(call) }
        delete("/{id}") { handler.delete(call) }
    }
}

class EventHttpHandler(private val eventService: EventService) {
    suspend fun list(call: ApplicationCall) {
        val date = parseDate(call.request.queryParameters["date"] ?: throw ValidationException("date is required"))
        val events = eventService.getEventsOn(date)
        call.respondProto(ListEventsResponse.newBuilder().addAllEvents(events.map { it.toProto() }).build())
    }

    suspend fun create(call: ApplicationCall) {
        val request = call.receiveProto(CreateEventRequest.parser())
        call.respondProto(eventService.createEvent(request.toCommand()).toProto(), HttpStatusCode.Created)
    }

    suspend fun delete(call: ApplicationCall) {
        eventService.deleteEvent(requireId(call.parameters["id"]))
        call.respond(HttpStatusCode.NoContent)
    }
}
```

## gRPC Handler

```kotlin
class EventGrpcHandler(private val eventService: EventService) : EventApiGrpcKt.EventApiCoroutineImplBase() {
    override suspend fun listEvents(request: ListEventsRequest): ListEventsResponse =
        grpcCall {
            val events = eventService.getEventsOn(parseDate(request.date))
            ListEventsResponse.newBuilder().addAllEvents(events.map { it.toProto() }).build()
        }

    override suspend fun createEvent(request: CreateEventRequest): EventMessage =
        grpcCall { eventService.createEvent(request.toCommand()).toProto() }

    override suspend fun deleteEvent(request: DeleteEventRequest): DeleteEventResponse =
        grpcCall {
            eventService.deleteEvent(requireId(request.id))
            DeleteEventResponse.getDefaultInstance()
        }
}
```

## 규칙

1. Handler는 생성자로 **Service만** 받는다. HTTP·gRPC Handler는 같은 Service, 같은 매퍼를 쓴다.
2. HTTP 메서드 시그니처는 `suspend fun xxx(call: ApplicationCall)`. 라우트 람다 안에 로직을 쓰지 않는다.
3. HTTP 바디는 `receiveProto` / `respondProto`만 사용 (`application/x-protobuf`).
4. HTTP 상태: 조회 200, 생성 201, 삭제 204. 4xx는 예외 → StatusPages.
5. gRPC 메서드는 전부 `grpcCall { }`로 감싼다.
6. 경로·상태 코드는 `proto-contract`의 매핑 표와 일치시킨다.

## 테스트

`ktor-mockk-testing`의 "HTTP Handler 테스트"(testApplication)와 "gRPC Handler 테스트"(InProcess) — 새 엔드포인트마다 **둘 다** 작성. 검증: 상태 코드/Status, 응답 proto, Service 인자(`coVerify`), 잘못된 입력 시 Service 미호출.
