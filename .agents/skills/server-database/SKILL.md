---
name: server-database
description: kotlin_calendar 서버의 데이터베이스 연결 규칙. 운영 DB는 Supabase(PostgreSQL)이고, 나중에 MySQL로 교체할 수 있도록 DatabaseProvider 인터페이스와 Koin 의존성 주입으로 연결을 만든다. DB 설정·환경변수·커넥션 풀·스키마 생성·DB 교체·로컬 H2 실행이 필요할 때 사용.
---

# 서버 데이터베이스 (Supabase → MySQL 교체 가능)

## 원칙

1. **DB 종류 선택은 `plugins/Database.kt`의 `databaseModule` 한 곳에서만** 한다.
2. Repository는 Exposed `Database`만 주입받고 **Exposed DSL만** 쓴다 (벤더 전용 SQL·타입 금지). 그래서 Supabase ↔ MySQL 교체 시 Repository 코드는 바뀌지 않는다.
3. 벤더별 차이(드라이버, SSL, 풀 옵션)는 `DatabaseProvider` 구현 안에만 둔다.

```
application.yaml (database.provider)
        │
        ▼
databaseModule(config) ── single<DatabaseProvider> ── SupabaseDatabaseProvider  (기본, PostgreSQL)
        │                                         ├─ MySqlDatabaseProvider     (교체 대상)
        │                                         └─ H2DatabaseProvider        (로컬·테스트)
        ▼
single<Database> { get<DatabaseProvider>().connect() }
        │
        ▼
ExposedEventRepository(database: Database)  ← 벤더 중립
```

## 배치

```
server/src/main/kotlin/com/kotlincalendar/server/
├── common/database/
│   ├── DatabaseProvider.kt      # interface DatabaseProvider : AutoCloseable { fun connect(): Database }
│   │                            # data class JdbcSettings, abstract class HikariDatabaseProvider
│   └── Providers.kt             # SupabaseDatabaseProvider, MySqlDatabaseProvider, H2DatabaseProvider
└── plugins/Database.kt          # databaseModule(config), databaseProviderFrom(config), configureDatabase()
```

## 인터페이스

```kotlin
interface DatabaseProvider : AutoCloseable {
    fun connect(): Database
}

abstract class HikariDatabaseProvider(private val settings: JdbcSettings) : DatabaseProvider {
    protected abstract val driverClassName: String
    protected open fun HikariConfig.customize() = Unit
    // connect(): HikariDataSource 생성 → Database.connect(dataSource), close(): 풀 종료
}

class SupabaseDatabaseProvider(settings: JdbcSettings) : HikariDatabaseProvider(settings) {
    override val driverClassName = "org.postgresql.Driver"
    override fun HikariConfig.customize() {
        addDataSourceProperty("sslmode", "require")
        addDataSourceProperty("prepareThreshold", "0") // transaction pooler(6543) 호환
    }
}

class MySqlDatabaseProvider(settings: JdbcSettings) : HikariDatabaseProvider(settings) {
    override val driverClassName = "com.mysql.cj.jdbc.Driver"
}
```

```kotlin
fun databaseModule(config: ApplicationConfig) = module {
    single<DatabaseProvider> { databaseProviderFrom(config) }
    single<Database> { get<DatabaseProvider>().connect() }
}

fun databaseProviderFrom(config: ApplicationConfig): DatabaseProvider =
    when (val provider = config.property("database.provider").getString().lowercase()) {
        "supabase" -> SupabaseDatabaseProvider(config.jdbcSettings("database.supabase"))
        "mysql" -> MySqlDatabaseProvider(config.jdbcSettings("database.mysql"))
        "h2" -> H2DatabaseProvider(config.property("database.h2.url").getString())
        else -> error("Unknown database.provider '$provider'")
    }
```

`configureDatabase()`는 `Database`를 주입받아 스키마를 만들고, `ApplicationStopped`에서 `provider.close()`로 풀을 닫는다.

## 설정 (`server/src/main/resources/application.yaml`)

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `DB_PROVIDER` | `supabase` | `supabase` \| `mysql` \| `h2` |
| `SUPABASE_DB_URL` | (없음) | Supabase 대시보드 → Connect → **Session pooler** JDBC URL. 예: `jdbc:postgresql://aws-0-ap-northeast-2.pooler.supabase.com:5432/postgres` |
| `SUPABASE_DB_USER` | (없음) | 보통 `postgres.<project-ref>` |
| `SUPABASE_DB_PASSWORD` | (없음) | DB 비밀번호 |
| `SUPABASE_DB_POOL_SIZE` | `5` | Supabase 무료 플랜은 연결 수가 적다 |
| `MYSQL_URL` / `MYSQL_USER` / `MYSQL_PASSWORD` | `jdbc:mysql://localhost:3306/calendar` / `root` / 빈값 | |
| `H2_URL` | `jdbc:h2:mem:calendar;DB_CLOSE_DELAY=-1` | |

- 비밀번호·URL은 **절대 커밋하지 않는다.** 로컬은 셸 환경변수나 IDE Run Configuration, CI·배포는 시크릿으로 넣는다.
- Supabase 접속 정보 없이 로컬에서 띄우려면 `DB_PROVIDER=h2`.

## 스키마 규칙 (벤더 중립)

- 시각: `long("xxx_at")` UTC epoch millis. (`timestamp` 컬럼은 벤더·드라이버별 타임존 동작이 달라 쓰지 않는다)
- 문자열: `varchar(name, length)` 길이 명시 (MySQL 인덱스 제한 대비), 긴 텍스트만 `text`.
- 테이블·컬럼 이름은 `snake_case`, SQL 예약어 피하기 (`end` → `end_at`).
- 현재는 시작 시 `SchemaUtils.create(...)`. 운영 데이터가 생기면 Flyway 마이그레이션으로 바꾸고 `db/migration/{postgresql,mysql}`로 나눈다.

## MySQL로 교체하는 절차

1. `DB_PROVIDER=mysql`, `MYSQL_*` 환경변수 설정 — **코드 변경 없음**
2. (데이터 이전) Supabase에서 덤프 → MySQL로 적재. 시각이 epoch millis라 변환 불필요.
3. Repository가 벤더 전용 기능을 쓰고 있지 않은지 확인: `grep -r "exec(\|PGobject\|jsonb" server/src/main`
4. Supabase 전용 기능(Auth, Storage, Realtime)을 쓰게 되면 그것도 `XxxProvider` 인터페이스 뒤로 숨긴다.

## 테스트

- `databaseProviderFrom(MapApplicationConfig(...))`가 provider 값별로 올바른 구현을 고르는지 단위 테스트.
- Repository 테스트는 `H2DatabaseProvider("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1")`로 실제 쿼리를 검증 (`ktor-mockk-testing`).
- CI는 Supabase에 접속하지 않는다.
