---
name: server
description: Ktor 서버 구조와 Handler·Service·Repository 계층 규칙. 패키지 배치, Application/플러그인/Koin 배선, proto↔도메인 매퍼, 비즈니스 검증, Exposed Repository. server/ 코드를 추가·수정할 때 읽는다.
---

# Ktor 서버

```
HttpHandler ─┐
             ├─▶ Service ─▶ Repository ─▶ Database
GrpcHandler ─┘
```

의존은 오른쪽으로만. **참고 구현은 `event/`, `user/` feature를 그대로 따른다.**

```
server/src/main/kotlin/com/kotlincalendar/server/
├── Application.kt          # module(): configureKoin/Database/Monitoring/Cors/StatusPages/Routing/Grpc
├── plugins/                # Koin.kt(feature 모듈), Database.kt, Http.kt, StatusPages.kt, Grpc.kt
├── common/                 # error/DomainException, http/ProtoHttp(receiveProto/respondProto), grpc/GrpcErrors(grpcCall), auth/, database/
└── <feature>/
    ├── domain/             # 모델, 커맨드
    ├── repository/         # XxxRepository(interface), ExposedXxxRepository, XxxTable
    ├── service/            # XxxService
    └── handler/            # XxxProtoMapper, http/(XxxRoutes, XxxHttpHandler), grpc/XxxGrpcHandler
```

## Handler (전송 ↔ 도메인 번역만)

- 생성자로 Service만 받는다. HTTP·gRPC Handler는 같은 Service·같은 매퍼(`XxxProtoMapper`).
- HTTP: `suspend fun x(call: ApplicationCall)`, 바디는 `receiveProto`/`respondProto`만 (JSON 금지). 라우트 람다에 로직 없음.
- gRPC: `XxxApiGrpcKt.XxxApiCoroutineImplBase` 상속, 모든 메서드를 `grpcCall { }`로 감싼다.
- 형식 오류(날짜 파싱, 빈 ID)는 매퍼에서 `ValidationException`. 예외를 직접 상태 코드로 바꾸지 않는다 (StatusPages/`grpcCall`).
- proto 클래스가 도메인과 이름이 겹치면 `as EventMessage` alias.

## Service (비즈니스 규칙의 유일한 장소)

- 생성자로 Repository **인터페이스**, `Clock`, `idGenerator`를 주입 (Koin에선 `single { XxxService(get()) }`).
- 입력은 커맨드, 출력은 도메인 모델, 실패는 `DomainException` 하위 예외. public 메서드는 `suspend`.
- `ApplicationCall`·proto·`HttpStatusCode`·Exposed를 import하지 않는다. 여러 Repository 원자 변경은 `TransactionRunner` 주입.

## Repository

- 인터페이스 + `ExposedXxxRepository(database: Database)`. 반환은 도메인 모델, 없음은 `null`/빈 리스트/`Boolean`.
- 모든 메서드 `newSuspendedTransaction(Dispatchers.IO, database)`. Exposed DSL만 (raw SQL·벤더 전용 타입 금지).
- 스키마는 Flyway 마이그레이션으로 만들고 `Table` 객체는 거기에 맞춘다 (`server-database`). `SchemaUtils.create` 금지.

## 새 feature 순서

proto(`proto-api`) → domain → repository + H2 테스트 → service + MockK 테스트 → handler(HTTP·gRPC) + 테스트 → Koin 모듈, `configureRouting`·`configureGrpc` 연결 → `./gradlew :server:test ktlintCheck`. 테스트 방법은 `server-testing`.
