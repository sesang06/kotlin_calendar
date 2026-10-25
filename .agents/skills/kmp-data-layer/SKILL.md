---
name: kmp-data-layer
description: kotlin_calendar 클라이언트의 domain/data 계층과 DI 규칙. 도메인 모델, Repository 인터페이스와 구현, Wire로 생성된 proto 메시지를 쓰는 RemoteDataSource(HTTP·gRPC), proto↔도메인 매핑, Ktor HttpClient, Koin 모듈 등록이 필요할 때 사용.
---

# KMP 데이터 계층

## 배치

```
client/shared/src/commonMain/kotlin/com/kotlincalendar/client/
├── domain/
│   ├── model/          # Event … 순수 data class (proto·직렬화 의존 없음)
│   └── repository/     # interface EventRepository
├── data/
│   ├── remote/
│   │   ├── ApiConfig.kt                 # httpBaseUrl, grpcBaseUrl, transport
│   │   ├── EventRemoteDataSource.kt     # interface (proto 메시지 입출력)
│   │   ├── http/HttpClientFactory.kt
│   │   ├── http/HttpEventRemoteDataSource.kt
│   │   ├── grpc/GrpcClientFactory.kt
│   │   └── grpc/GrpcEventRemoteDataSource.kt
│   ├── mapper/EventProtoMapper.kt       # proto Event ↔ domain Event
│   └── repository/EventRepositoryImpl.kt
└── di/
    ├── AppModule.kt
    └── PlatformModule.kt                # expect val platformModule: Module
```

메시지 클래스는 손으로 만들지 않는다. Wire가 `proto/`에서 생성한 `com.kotlincalendar.proto.calendar.v1.*`를 쓴다 (`proto-contract`). 전송 방식 선택은 `api-transport`.

## 규칙

1. **domain은 data를 모른다.** domain에 Ktor·Wire·proto import 금지.
2. **proto 메시지는 data 밖으로 나가지 않는다.** Repository 구현에서 도메인 모델로 매핑해서 반환.
3. Repository는 `suspend fun ...: Result<T>`. 예외를 ViewModel까지 던지지 않는다. 단 `CancellationException`은 다시 던진다.
4. 시각은 `kotlin.time.Instant` (opt-in `ExperimentalTime`), 날짜는 `kotlinx.datetime.LocalDate`. proto에서는 `google.protobuf.Timestamp` / `yyyy-MM-dd` 문자열.
5. API 주소·전송 방식은 `ApiConfig`로 주입 (플랫폼별: Android 에뮬레이터 `10.0.2.2`, 그 외 `localhost`; HTTP 8080, gRPC 50051).

## HttpClient

```kotlin
fun createHttpClient(engine: HttpClientEngine, config: ApiConfig) = HttpClient(engine) {
    install(Logging) { level = LogLevel.INFO }
    defaultRequest { url(config.httpBaseUrl) }
    expectSuccess = true
}
```

엔진은 `platformModule`이 제공: android·desktop → `OkHttp`, ios → `Darwin`, wasmJs → `Js`. JSON ContentNegotiation은 쓰지 않는다.

## RemoteDataSource / Repository 템플릿

```kotlin
interface EventRemoteDataSource {
    suspend fun listEvents(request: ListEventsRequest): ListEventsResponse
    suspend fun createEvent(request: CreateEventRequest): EventMessage
}

class HttpEventRemoteDataSource(private val client: HttpClient) : EventRemoteDataSource {
    override suspend fun listEvents(request: ListEventsRequest): ListEventsResponse {
        val bytes = client.get("/api/events") { parameter("date", request.date) }.body<ByteArray>()
        return ListEventsResponse.ADAPTER.decode(bytes)
    }

    override suspend fun createEvent(request: CreateEventRequest): EventMessage {
        val bytes = client.post("/api/events") {
            contentType(ContentType("application", "x-protobuf"))
            setBody(request.encode())
        }.body<ByteArray>()
        return EventMessage.ADAPTER.decode(bytes)
    }
}

class GrpcEventRemoteDataSource(private val api: EventApiClient) : EventRemoteDataSource {
    override suspend fun listEvents(request: ListEventsRequest) = api.ListEvents().execute(request)
    override suspend fun createEvent(request: CreateEventRequest) = api.CreateEvent().execute(request)
}

class EventRepositoryImpl(private val remote: EventRemoteDataSource) : EventRepository {
    override suspend fun getEvents(date: LocalDate): Result<List<Event>> =
        runCatchingCancellable { remote.listEvents(ListEventsRequest(date = date.toString())).events.map { it.toDomain() } }
}
```

Wire 생성 클래스의 필드명은 proto 그대로 snake_case다 (`event.start_time`, `Event(start_time = ...)`). 이 이름은 `data/mapper`에서만 다루고, 도메인 모델은 camelCase로 둔다.

```kotlin
fun EventMessage.toDomain() = Event(
    id = id,
    title = title,
    start = start_time?.let { Instant.fromEpochSeconds(it.seconds, it.nanos) } ?: Instant.DISTANT_PAST,
    end = end_time?.let { Instant.fromEpochSeconds(it.seconds, it.nanos) } ?: Instant.DISTANT_PAST,
)
```

생성 위치: `client/shared/build/generated/source/wire/` (서버 protoc 코드는 `server/build/generated/sources/proto/main/{java,grpc,grpckt}/`).

## Koin

```kotlin
val appModule = module {
    single { createHttpClient(get(), get()) }
    single<EventRemoteDataSource> { /* api-transport의 전송 방식 선택 */ }
    singleOf(::EventRepositoryImpl) bind EventRepository::class
    viewModelOf(::CalendarViewModel)
}

fun initKoin(extra: KoinAppDeclaration = {}) = startKoin {
    extra()
    modules(appModule, platformModule)
}
```

`platformModule`이 제공하는 것: `ApiConfig`, `HttpClientEngine`, `GrpcClientFactory`. 각 진입점에서 `initKoin()`을 한 번 호출한다.
