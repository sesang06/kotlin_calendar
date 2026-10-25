---
name: kmp-testing
description: kotlin_calendar 클라이언트 공통 로직 테스트 규칙. client/shared의 commonTest에서 ViewModel, Repository, 매퍼를 테스트할 때 사용. Fake 작성, 코루틴 테스트 디스패처, Turbine으로 StateFlow/Effect 검증 방법.
---

# KMP 테스트

## 도구

- `kotlin-test`, `kotlinx-coroutines-test`, `app.cash.turbine:turbine`
- HTTP RemoteDataSource 테스트: `io.ktor:ktor-client-mock` (`MockEngine`)이 Wire로 인코딩한 proto 바이트를 응답
- **MockK는 JVM 전용이라 commonTest에서 쓰지 않는다.** 대신 수작업 Fake를 만든다.

## 배치

```
client/shared/src/commonTest/kotlin/com/kotlincalendar/client/
├── fake/                       # FakeEventRepository, FixedClock
├── presentation/<feature>/     # <Feature>ViewModelTest
└── data/                       # Repository/Api/매퍼 테스트
```

## 규칙

1. ViewModel 테스트는 `Dispatchers.setMain(StandardTestDispatcher())`, 끝나면 `resetMain()`.
2. 상태 검증은 Turbine `state.test { }`, Effect는 `effects.test { assertEquals(..., awaitItem()) }`.
3. Fake는 인터페이스를 구현하고, 호출 기록(`calls`)과 응답 설정(`result`)을 필드로 노출.
4. 시간은 `FixedClock`으로 고정.
5. 테스트 이름은 한글 백틱 문장 허용 (단, iOS/wasm 타깃 함수명 제약 때문에 `@Test fun selectDate_loadsEvents()` 형식도 OK — 백틱 이름이 빌드 오류를 내면 영문으로).

## 템플릿

```kotlin
class FakeEventRepository : EventRepository {
    var result: Result<List<Event>> = Result.success(emptyList())
    val requestedDates = mutableListOf<LocalDate>()

    override suspend fun getEvents(date: LocalDate): Result<List<Event>> {
        requestedDates += date
        return result
    }
}

// kotlin.time.Clock / kotlin.time.Instant
class FixedClock(private val instant: Instant) : Clock {
    override fun now() = instant
}
```

### HTTP RemoteDataSource (MockEngine + proto)

```kotlin
@Test
fun listEvents_decodesProtoBody() = runTest {
    val engine = MockEngine { request ->
        assertEquals("2026-10-25", request.url.parameters["date"])
        respond(
            content = ListEventsResponse(events = listOf(sampleEventMessage)).encode(),
            headers = headersOf(HttpHeaders.ContentType, "application/x-protobuf"),
        )
    }
    val source = HttpEventRemoteDataSource(HttpClient(engine))

    val response = source.listEvents(ListEventsRequest(date = "2026-10-25"))

    assertEquals(listOf(sampleEventMessage), response.events)
}
```

gRPC RemoteDataSource는 Wire 생성 `EventApiClient` 인터페이스를 Fake로 구현해서 테스트한다.

```kotlin
class CalendarViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeEventRepository()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun selectDate_loadsEventsForDate() = runTest(dispatcher) {
        repo.result = Result.success(listOf(sampleEvent))
        val vm = CalendarViewModel(repo, FixedClock(today))

        vm.onAction(CalendarAction.SelectDate(otherDate))
        advanceUntilIdle()

        assertEquals(listOf(sampleEvent), vm.state.value.events)
        assertEquals(otherDate, repo.requestedDates.last())
    }

    @Test
    fun repositoryFailure_setsErrorMessage() = runTest(dispatcher) {
        repo.result = Result.failure(IllegalStateException("boom"))
        val vm = CalendarViewModel(repo, FixedClock(today))
        advanceUntilIdle()

        assertEquals("boom", vm.state.value.errorMessage)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun openEvent_emitsNavigateEffect() = runTest(dispatcher) {
        val vm = CalendarViewModel(repo, FixedClock(today))
        vm.effects.test {
            vm.onAction(CalendarAction.OpenEvent("e1"))
            assertEquals(CalendarEffect.NavigateToEvent("e1"), awaitItem())
        }
    }
}
```

## 실행

```bash
./gradlew :client:shared:allTests
./gradlew :client:shared:desktopTest   # 가장 빠른 피드백
```
