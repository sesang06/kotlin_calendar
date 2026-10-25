---
name: ktor-repository
description: kotlin_calendar 서버의 Repository 계층 규칙. 데이터 접근 인터페이스 정의, Exposed 테이블·구현체 작성, 도메인 모델 매핑, newSuspendedTransaction 처리가 필요할 때 사용. 테이블/쿼리를 추가하거나 고칠 때 사용. DB 연결·설정은 server-database 스킬.
---

# Repository 계층

Repository는 **영속성 세부사항을 숨기는 경계**다. 비즈니스 판단을 하지 않는다. 어떤 DB에 붙는지는 모른다 (`server-database`가 주입하는 `Database`만 안다).

## 구조

```
<feature>/repository/
├── EventRepository.kt          # interface — Service가 의존하는 것
├── ExposedEventRepository.kt   # Exposed 구현 (생성자: Database)
└── EventTable.kt               # Table 정의 + ResultRow.toEvent()
```

## 템플릿

```kotlin
interface EventRepository {
    suspend fun findById(id: String): Event?
    suspend fun findByDate(date: LocalDate): List<Event>
    suspend fun existsOverlapping(start: Instant, end: Instant): Boolean
    suspend fun save(event: Event): Event
    suspend fun deleteById(id: String): Boolean
}
```

```kotlin
/** 시각은 TIMESTAMPTZ(PostgreSQL) 컬럼에 매핑한다. */
object EventTable : Table("events") {
    val id = varchar("id", 36)
    val title = varchar("title", 200)
    val startAt = timestamp("start_at")
    val endAt = timestamp("end_at")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
    init { index(isUnique = false, startAt, endAt) }
}

fun ResultRow.toEvent() = Event(
    id = this[EventTable.id],
    title = this[EventTable.title],
    start = this[EventTable.startAt],
    end = this[EventTable.endAt],
    createdAt = this[EventTable.createdAt],
)
```

```kotlin
class ExposedEventRepository(private val database: Database) : EventRepository {

    private suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }

    override suspend fun findById(id: String): Event? = dbQuery {
        EventTable.selectAll().where { EventTable.id eq id }.singleOrNull()?.toEvent()
    }

    override suspend fun existsOverlapping(start: Instant, end: Instant): Boolean = dbQuery {
        EventTable.selectAll()
            .where { (EventTable.startAt less end) and (EventTable.endAt greater start) }
            .limit(1)
            .any()
    }

    override suspend fun deleteById(id: String): Boolean = dbQuery {
        EventTable.deleteWhere { EventTable.id eq id } > 0
    }
    // findByDate, save …
}
```

`deleteWhere` 람다 안의 `eq`는 `import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq`가 필요하다 (와일드카드 import 금지 — `ktlint`).

## 규칙

1. **인터페이스와 구현을 분리**한다. Service·Koin 바인딩은 인터페이스 타입으로.
2. 생성자로 `Database`를 받는다. `Database.connect`·드라이버·URL을 Repository에서 직접 다루지 않는다.
3. **Exposed DSL만** 사용: `exec("raw sql")`, 특정 DB 전용 타입(jsonb 등) 금지.
4. 반환 타입은 **도메인 모델**. `ResultRow`·Exposed 타입이 계층 밖으로 새지 않는다.
5. 없음은 `null` / 빈 리스트 / `Boolean`으로 표현. `NotFoundException`은 Service 몫.
6. 모든 메서드는 `suspend` + `newSuspendedTransaction(Dispatchers.IO, database)`.
7. 컬럼 규칙(시각=`timestamp`, `varchar` 길이 명시, 예약어 회피)은 `server-database`의 "스키마 규칙".
8. 새 테이블·컬럼은 **Flyway 마이그레이션**(`db/migration/postgresql`)으로 추가하고, Exposed `Table` 객체는 그 스키마에 맞춘다. `SchemaUtils.create`는 쓰지 않는다 (`server-database`의 "Flyway 마이그레이션").

## 테스트

- Repository는 MockK로 모킹하지 않고 **H2 인메모리 DB로 실제 쿼리를 검증**한다 (`ktor-mockk-testing`의 "Repository 테스트").
- Repository를 **사용하는 쪽**(Service)의 테스트에서는 이 인터페이스를 MockK로 모킹한다.
