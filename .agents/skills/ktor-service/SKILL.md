---
name: ktor-service
description: kotlin_calendar 서버의 Service 계층 규칙. 비즈니스 로직, 도메인 검증, 여러 Repository 조합, 트랜잭션 경계, 도메인 예외 던지기를 다룰 때 사용. 캘린더 일정 충돌 검사·권한 확인 같은 규칙을 구현하거나 고칠 때 사용.
---

# Service 계층

Service는 **비즈니스 규칙의 유일한 장소**다. HTTP도 DB 구현도 모른다.

## 책임

- 할 일: 도메인 검증(예: 종료 시각 > 시작 시각), 규칙(일정 겹침, 권한), 여러 Repository 조합, ID·시각 생성, 실패 시 `DomainException` 던지기
- 하지 않을 일: `ApplicationCall`/DTO/`HttpStatusCode` 사용, SQL·Exposed 직접 사용

## 템플릿

```kotlin
class EventService(
    private val eventRepository: EventRepository,
    private val clock: Clock = Clock.System,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    suspend fun getEventsOn(date: LocalDate): List<Event> =
        eventRepository.findByDate(date)

    suspend fun getEvent(id: String): Event =
        eventRepository.findById(id) ?: throw NotFoundException("event $id not found")

    suspend fun createEvent(command: CreateEventCommand): Event {
        if (command.title.isBlank()) throw ValidationException("title must not be blank")
        if (command.end <= command.start) throw ValidationException("end must be after start")
        if (eventRepository.existsOverlapping(command.start, command.end)) {
            throw ConflictException("event overlaps existing event")
        }
        val event = Event(
            id = idGenerator(),
            title = command.title.trim(),
            start = command.start,
            end = command.end,
            createdAt = clock.now(),
        )
        return eventRepository.save(event)
    }

    suspend fun deleteEvent(id: String) {
        if (!eventRepository.deleteById(id)) throw NotFoundException("event $id not found")
    }
}
```

## 규칙

1. 생성자로 **Repository 인터페이스**(구현체 아님)와 `Clock`, ID 생성기 등 비결정적 요소를 주입받는다 → MockK/고정값으로 테스트 가능.
2. 입력은 도메인 커맨드(`CreateEventCommand`), 출력은 도메인 모델(`Event`).
3. 실패는 반환값이 아니라 `DomainException` 하위 예외로 표현 (StatusPages가 HTTP로 변환).
4. 모든 public 메서드는 `suspend`.
5. 여러 Repository를 원자적으로 바꿔야 하면 `TransactionRunner` 인터페이스를 주입받아 `transactionRunner { ... }`로 감싼다 (Exposed 의존을 Service에 들이지 않기 위해).

```kotlin
interface TransactionRunner { suspend operator fun <T> invoke(block: suspend () -> T): T }
```

## 테스트 (필수)

Service마다 `ktor-mockk-testing`의 "Service 단위 테스트" 형식으로 작성한다. 최소한:

- 정상 경로: 반환값 + Repository 호출 인자 검증 (`coVerify`)
- 각 검증 실패: 올바른 예외 타입 + **Repository 저장 미호출** (`coVerify(exactly = 0) { repo.save(any()) }`)
- 없는 리소스: `NotFoundException`
