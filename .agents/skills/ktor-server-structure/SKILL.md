---
name: ktor-server-structure
description: kotlin_calendar 서버(Ktor)의 전체 구조와 배선 규칙. Handler·Service·Repository 3계층 패키지 배치, Application 모듈, 플러그인(StatusPages, CORS, gRPC 서버), Koin DI 등록, 라우팅 연결이 필요할 때 사용. server/ 아래에 새 기능을 추가하거나 Application.kt·build.gradle.kts를 고칠 때 먼저 읽는다.
---

# Ktor 서버 구조

## 계층과 의존 방향

```
HTTP  ─▶ HttpHandler ─┐
                      ├─▶ Service ─▶ Repository ─▶ Database(Supabase/H2)
gRPC  ─▶ GrpcHandler ─┘
         (proto 변환)     (비즈니스)   (데이터 접근)
```

- 의존은 **오른쪽으로만**. Repository가 Service를, Service가 Handler·`ApplicationCall`·proto 클래스를 알면 안 된다.
- Handler ↔ 외부: **proto 메시지** (`proto-contract`). Handler ↔ Service: **도메인 모델/커맨드**.
- 상세: `ktor-handler`, `api-transport`, `ktor-service`, `ktor-repository`, `server-database`. 테스트: `ktor-mockk-testing`.

## 패키지

```
server/src/main/kotlin/com/kotlincalendar/server/
├── Application.kt                  # main + Application.module()
├── plugins/
│   ├── Koin.kt                     # configureKoin(modules), feature 모듈(eventModule …)
│   ├── Database.kt                 # databaseModule(config), configureDatabase()
│   ├── Http.kt                     # configureMonitoring/Cors/StatusPages/Routing
│   └── Grpc.kt                     # configureGrpc(): 별도 포트 gRPC 서버
├── common/
│   ├── error/DomainException.kt    # 예외 계층 + toHttpStatus()/toGrpcStatus()
│   ├── http/ProtoHttp.kt           # receiveProto / respondProto
│   ├── grpc/GrpcErrors.kt          # grpcCall { }
│   └── database/                   # DatabaseProvider 구현들
└── <feature>/                      # 예: event/
    ├── domain/      Event.kt, CreateEventCommand.kt
    ├── repository/  EventRepository.kt (interface), ExposedEventRepository.kt, EventTable.kt
    ├── service/     EventService.kt
    └── handler/     EventProtoMapper.kt, http/EventHttpHandler.kt, http/EventRoutes.kt, grpc/EventGrpcHandler.kt
server/src/test/kotlin/com/kotlincalendar/server/<feature>/...   # 같은 패키지 구조
```

**feature 단위로 묶고, 그 안에서 계층을 나눈다.**

## 스택 (`server/build.gradle.kts`, 버전은 `libs.versions.toml`)

- Ktor: `server-core`, `server-netty`, `server-config-yaml`, `server-status-pages`, `server-call-logging`, `server-cors`
- 통신: `protobuf-java`, `grpc-netty-shaded`, `grpc-protobuf`, `grpc-stub`, `grpc-kotlin-stub` + `com.google.protobuf` 플러그인
- DI: `koin-ktor`, `koin-logger-slf4j`
- DB: Exposed(`core`, `jdbc`), HikariCP, PostgreSQL 드라이버(Supabase), H2
- 시간: `kotlin.time.Instant/Clock` (opt-in `kotlin.time.ExperimentalTime`), 날짜 `kotlinx-datetime`
- 테스트: JUnit5, MockK, `ktor-server-test-host`, `grpc-inprocess`, `kotlinx-coroutines-test`
- JSON ContentNegotiation은 쓰지 않는다 (바디는 protobuf).

## Application

```kotlin
fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    configureKoin(listOf(databaseModule(environment.config), eventModule))
    configureDatabase()
    configureMonitoring()
    configureCors()
    configureStatusPages()
    configureRouting()
    configureGrpc()
}
```

각 `configureXxx()`는 독립적으로 호출 가능해야 한다 → 테스트에서 필요한 것만 조립 (Handler 테스트는 DB·gRPC 없이 `configureKoin + configureStatusPages + configureRouting`).

```kotlin
// plugins/Koin.kt
val eventModule = module {
    singleOf(::ExposedEventRepository) bind EventRepository::class
    single { EventService(get()) }          // 기본 인자(Clock, idGenerator)가 있으면 singleOf 대신 람다
    singleOf(::EventHttpHandler)
    singleOf(::EventGrpcHandler)
}
```

## 설정 (`application.yaml`)

| 키 | 환경변수 | 기본 |
|---|---|---|
| `ktor.deployment.port` | `PORT` | 8080 |
| `grpc.port` | `GRPC_PORT` | 50051 |
| `cors.hosts` | `CORS_HOSTS` | `localhost:8081,localhost:8080` |
| `database.*` | `DB_PROVIDER`, `SUPABASE_DB_*` … | `server-database` 참고 |

## 새 feature 추가 순서

1. `proto/`에 메시지·RPC 추가 (`proto-contract`)
2. `domain/` 모델·커맨드
3. `repository/` 인터페이스 → Exposed 구현 + H2 테스트 (`ktor-repository`)
4. `service/` + MockK 단위 테스트 (`ktor-service`)
5. `handler/` 매퍼 + HTTP·gRPC Handler + 테스트 (`ktor-handler`, `api-transport`)
6. Koin 모듈 등록, `configureRouting`·`configureGrpc`에 연결
7. `./gradlew :server:test ktlintCheck`
