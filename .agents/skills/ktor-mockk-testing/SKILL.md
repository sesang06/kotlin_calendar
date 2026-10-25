---
name: ktor-mockk-testing
description: kotlin_calendar 서버 테스트 규칙. MockK로 Service 단위 테스트(Repository 모킹), testApplication으로 HTTP Handler 테스트(Service 모킹, proto 바디), InProcess gRPC로 gRPC Handler 테스트, H2로 Repository 테스트, DatabaseProvider 선택 테스트를 작성할 때 사용. server/src/test 아래 테스트를 만들거나 고칠 때, mockk/coEvery/coVerify/slot 사용법이 필요할 때 사용.
---

# 서버 테스트 (MockK)

## 계층별 전략

| 대상 | 무엇을 모킹 | 도구 |
|---|---|---|
| Service | Repository (MockK) | JUnit5 + MockK + `runTest` |
| HTTP Handler | Service (MockK) | `testApplication` + MockK, proto 바이트 바디 |
| gRPC Handler | Service (MockK) | `InProcessServerBuilder` + `EventApiCoroutineStub` + MockK |
| Repository | 모킹 없음 | `H2DatabaseProvider` (인메모리) |
| DB 선택 | 모킹 없음 | `databaseProviderFrom(MapApplicationConfig(...))` |

**바로 아래 계층만 모킹한다.** 두 계층 건너뛰어 모킹하지 않는다. 외부 DB(Supabase)·네트워크에 접속하는 테스트는 만들지 않는다.

## MockK 핵심 규칙

- suspend 함수는 **`coEvery` / `coVerify`**. (일반 `every`로 suspend 함수를 스텁하지 않는다)
- 기본은 strict `mockk<T>()` — 스텁 안 한 호출은 실패해야 의도치 않은 호출이 드러난다. `relaxed = true`는 반환값이 무의미한 `Unit` 위주일 때만.
- 인자 캡처는 `slot<T>()` + `capture(slot)`.
- 호출 안 됨 검증: `coVerify(exactly = 0) { ... }`. 다 검증했으면 `confirmVerified(mock)`.
- 테스트마다 새 mock. 전역 mock(`mockkObject`, `mockkStatic`)을 쓰면 반드시 `unmockkAll()`.
- 시간·ID는 MockK 대신 **고정값 주입**(`object : Clock { override fun now() = fixed }`, `idGenerator = { "id-1" }`).

## Service 단위 테스트

```kotlin
class EventServiceTest {
    private val repo = mockk<EventRepository>()
    private val now = Instant.parse("2026-10-25T00:00:00Z")
    private val service = EventService(repo, clock = object : Clock { override fun now() = now }, idGenerator = { "id-1" })

    private val command = CreateEventCommand(
        title = "회의",
        start = Instant.parse("2026-10-26T01:00:00Z"),
        end = Instant.parse("2026-10-26T02:00:00Z"),
    )

    @Test
    fun `일정을 생성하면 저장된 일정을 반환한다`() = runTest {
        val saved = slot<Event>()
        coEvery { repo.existsOverlapping(command.start, command.end) } returns false
        coEvery { repo.save(capture(saved)) } answers { saved.captured }

        val result = service.createEvent(command)

        assertEquals("id-1", result.id)
        assertEquals(now, saved.captured.createdAt)
    }

    @Test
    fun `종료가 시작보다 빠르면 ValidationException이고 저장하지 않는다`() = runTest {
        assertFailsWith<ValidationException> { service.createEvent(command.copy(end = command.start)) }
        coVerify(exactly = 0) { repo.save(any()) }
    }
}
```

## HTTP Handler 테스트

```kotlin
class EventHttpHandlerTest {
    private val service = mockk<EventService>()

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        // application.yaml 자동 로드를 막는다 (Supabase 접속 방지)
        environment { config = MapApplicationConfig() }
        application {
            configureKoin(listOf(module {
                single { service }
                singleOf(::EventHttpHandler)
            }))
            configureStatusPages()
            configureRouting()
        }
        block()
    }

    @Test
    fun `POST events는 201과 생성된 일정을 proto로 반환한다`() = withApp {
        coEvery { service.createEvent(any()) } returns event

        val response = client.post("/api/events") {
            contentType(ProtobufContentType)
            setBody(CreateEventRequest.newBuilder().setTitle("회의").setStartTime(s).setEndTime(e).build().toByteArray())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("id-1", EventMessage.parseFrom(response.body<ByteArray>()).id)
    }

    @Test
    fun `Service가 NotFoundException을 던지면 404와 ErrorResponse`() = withApp {
        coEvery { service.getEvent("x") } throws NotFoundException("nope")

        val response = client.get("/api/events/x")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("nope", ErrorResponse.parseFrom(response.body<ByteArray>()).message)
    }

    @Test
    fun `date 형식이 잘못되면 400이고 Service를 호출하지 않는다`() = withApp {
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/events?date=bad").status)
        coVerify(exactly = 0) { service.getEventsOn(any()) }
    }
}
```

## gRPC Handler 테스트

```kotlin
class EventGrpcHandlerTest {
    private val service = mockk<EventService>()
    private val serverName = InProcessServerBuilder.generateName()
    private val server = InProcessServerBuilder.forName(serverName).directExecutor()
        .addService(EventGrpcHandler(service)).build().start()
    private val channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
    private val stub = EventApiGrpcKt.EventApiCoroutineStub(channel)

    @AfterTest
    fun tearDown() {
        channel.shutdownNow()
        server.shutdownNow()
    }

    @Test
    fun `없는 일정이면 NOT_FOUND`() = runTest {
        coEvery { service.getEvent("x") } throws NotFoundException("nope")

        val e = assertFailsWith<StatusException> { stub.getEvent(GetEventRequest.newBuilder().setId("x").build()) }

        assertEquals(Status.Code.NOT_FOUND, e.status.code)
    }
}
```

## Repository 테스트 (H2)

```kotlin
class ExposedEventRepositoryTest {
    // 운영과 같은 스키마를 쓰기 위해 Flyway 마이그레이션을 적용한다 (SchemaUtils.create 금지).
    private val provider = H2DatabaseProvider("jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
    private val database = provider.connect().also { DatabaseMigrator(provider.dataSource(), DatabaseVendor.POSTGRESQL).migrate() }
    private val repo = ExposedEventRepository(database)

    @Test
    fun `겹치는 구간이 있으면 true, 경계가 맞닿기만 하면 false`() = runTest {
        repo.save(event(start = at("10:00"), end = at("11:00")))
        assertTrue(repo.existsOverlapping(at("10:30"), at("11:30")))
        assertFalse(repo.existsOverlapping(at("11:00"), at("12:00")))
    }
}
```

## DB 선택 테스트

```kotlin
@Test
fun `provider가 mysql이면 MySqlDatabaseProvider`() {
    val config = MapApplicationConfig(
        "database.provider" to "mysql",
        "database.mysql.url" to "jdbc:mysql://localhost/x",
        "database.mysql.user" to "u",
        "database.mysql.password" to "p",
        "database.mysql.maxPoolSize" to "2",
    )
    assertIs<MySqlDatabaseProvider>(databaseProviderFrom(config))
}
```

## 네이밍 / 위치

- 테스트 클래스: `<대상>Test`, 프로덕션과 같은 패키지.
- 테스트 이름: 백틱 한글 문장 — "조건이면 결과".
- Given / When / Then 을 빈 줄로 구분.

## 실행

```bash
./gradlew :server:test
./gradlew :server:test --tests "*EventServiceTest"
```
