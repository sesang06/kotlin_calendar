---
name: api-transport
description: kotlin_calendar의 클라이언트↔서버 통신 방식(gRPC 또는 HTTP) 규칙. 서버가 같은 Service를 gRPC Handler와 HTTP Handler 두 가지로 노출하는 방법, 클라이언트가 RemoteDataSource 인터페이스 뒤에서 HTTP/gRPC 구현을 고르는 방법, 포트·Content-Type·에러 매핑·플랫폼별 제약(웹은 HTTP 전용)이 필요할 때 사용.
---

# 통신 방식: gRPC 또는 HTTP

메시지는 항상 proto(`proto-contract`), 전송은 **gRPC** 또는 **HTTP(protobuf 바이너리)** 중 하나를 고른다. 비즈니스 로직은 전송 방식을 모른다.

```
                ┌─ EventGrpcHandler (grpc-kotlin, :50051) ─┐
Client ──proto──┤                                          ├─▶ EventService ─▶ EventRepository
                └─ EventHttpHandler (Ktor, :8080)  ────────┘
```

## 서버

### 배치

```
server/src/main/kotlin/com/kotlincalendar/server/
├── common/
│   ├── http/ProtoHttp.kt        # receiveProto(parser), respondProto(msg, status), ProtobufContentType
│   ├── grpc/GrpcErrors.kt       # grpcCall { } : DomainException → StatusException
│   └── error/DomainException.kt # toHttpStatus(), toGrpcStatus()
├── plugins/Grpc.kt              # configureGrpc(): NettyServerBuilder로 별도 포트 기동
└── <feature>/handler/
    ├── <Feature>ProtoMapper.kt  # proto ↔ 도메인 (HTTP·gRPC 공용)
    ├── http/<Feature>HttpHandler.kt, <Feature>Routes.kt
    └── grpc/<Feature>GrpcHandler.kt
```

### 규칙

1. **두 Handler는 같은 Service를 호출**하고, 같은 매퍼를 쓴다. 한쪽에만 있는 로직을 만들지 않는다.
2. gRPC Handler는 생성된 `XxxApiGrpcKt.XxxApiCoroutineImplBase`를 상속하고, 모든 메서드 본문을 `grpcCall { }`로 감싼다.
3. HTTP Handler는 `call.receiveProto(Xxx.parser())` / `call.respondProto(msg)`만 쓴다. JSON ContentNegotiation을 쓰지 않는다.
4. 에러 매핑은 한 곳씩:

| DomainException | HTTP (StatusPages) | gRPC (grpcCall) |
|---|---|---|
| `ValidationException` | 400 + `ErrorResponse` | `INVALID_ARGUMENT` |
| `NotFoundException` | 404 + `ErrorResponse` | `NOT_FOUND` |
| `ConflictException` | 409 + `ErrorResponse` | `ALREADY_EXISTS` |
| `UnauthenticatedException` | 401 + `ErrorResponse` | `UNAUTHENTICATED` |

5. **인증**: access token은 HTTP `Authorization: Bearer <token>` 헤더, gRPC metadata `authorization: Bearer <token>`으로 받는다. 검증은 전송 계층 한 곳(HTTP: Ktor Authentication 플러그인, gRPC: `ServerInterceptor`)에서 하고, Handler는 검증된 사용자 ID만 받아 Service에 넘긴다. `AuthApi`의 SignUp·Login·RefreshToken만 인증 없이 허용.
6. 요청 형식 오류(날짜 파싱 등)는 매퍼에서 `ValidationException`으로 던져 두 전송 방식이 같은 결과를 내게 한다.
7. 포트: HTTP `ktor.deployment.port`(기본 8080), gRPC `grpc.port`(기본 50051). gRPC 서버는 `ApplicationStarted`에 start, `ApplicationStopping`에 shutdown.
8. CORS: `Authorization` 헤더도 `allowHeader`에 추가한다. `application/x-protobuf`는 non-simple content type이므로 `allowNonSimpleContentTypes = true`.

### 템플릿

```kotlin
class EventGrpcHandler(private val eventService: EventService) : EventApiGrpcKt.EventApiCoroutineImplBase() {
    override suspend fun getEvent(request: GetEventRequest): EventMessage =
        grpcCall { eventService.getEvent(requireId(request.id)).toProto() }
}

class EventHttpHandler(private val eventService: EventService) {
    suspend fun get(call: ApplicationCall) {
        call.respondProto(eventService.getEvent(requireId(call.parameters["id"])).toProto())
    }
}
```

## 클라이언트

### 배치

```
client/shared/src/commonMain/kotlin/com/kotlincalendar/client/data/remote/
├── ApiConfig.kt                    # httpBaseUrl, grpcBaseUrl, transport: Transport
├── EventRemoteDataSource.kt        # interface — Repository가 의존
├── http/HttpEventRemoteDataSource.kt   # Ktor Client + proto 바이트
├── grpc/GrpcEventRemoteDataSource.kt   # Wire 생성 GrpcEventApiClient
└── grpc/GrpcClientFactory.kt       # fun interface: (baseUrl) -> GrpcClient?  (platformModule에서 제공)
```

### 규칙

1. Repository는 `EventRemoteDataSource` 인터페이스만 안다. 전송 방식 선택은 **Koin 모듈 한 곳**에서 `ApiConfig.transport`로 한다.
2. HTTP 구현: `setBody(request.encode())` + `contentType(application/x-protobuf)`, 응답은 `Xxx.ADAPTER.decode(response.body<ByteArray>())`.
3. gRPC 구현: Wire 생성 `GrpcEventApiClient(grpcClient).GetEvent().execute(request)`.
4. **플랫폼 제약**:

| 플랫폼 | HTTP | gRPC |
|---|---|---|
| Android | ✅ | ✅ (OkHttp, h2c) |
| Desktop(JVM) | ✅ | ✅ (OkHttp, h2c) |
| iOS | ✅ | ⚠️ `GrpcClientFactory`가 null이면 HTTP로 대체 |
| Web(wasmJs) | ✅ | ❌ 브라우저는 순수 gRPC 불가 (gRPC-Web 프록시 필요) → HTTP |

5. `GrpcClientFactory`가 `null`을 돌려주는 플랫폼은 자동으로 HTTP 구현을 쓴다. 기본 transport는 `HTTP`.
6. 로컬 개발 gRPC는 평문 HTTP/2(h2c): OkHttp `protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))`.

```kotlin
single<EventRemoteDataSource> {
    val config = get<ApiConfig>()
    val grpcClient = if (config.transport == Transport.GRPC) get<GrpcClientFactory>().create(config.grpcBaseUrl) else null
    grpcClient?.let { GrpcEventRemoteDataSource(GrpcEventApiClient(it)) }
        ?: HttpEventRemoteDataSource(get())
}
```

## 테스트

- 서버 HTTP Handler: `testApplication` + MockK Service, proto 바이트로 요청/검증 (`ktor-mockk-testing`).
- 서버 gRPC Handler: `InProcessServerBuilder` + MockK Service + `EventApiCoroutineStub`, `StatusException.status.code` 검증.
- 클라이언트 HTTP 구현: Ktor `MockEngine`이 proto 바이트를 돌려주게 해서 검증 (`kmp-testing`).
- 새 RPC를 추가하면 **HTTP와 gRPC 양쪽 Handler 테스트를 모두** 추가한다.
