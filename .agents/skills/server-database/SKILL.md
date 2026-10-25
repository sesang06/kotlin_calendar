---
name: server-database
description: kotlin_calendar 서버의 데이터베이스 연결 규칙. 운영 DB는 Supabase(PostgreSQL)이고, 로컬·테스트에서는 H2(PostgreSQL 모드)를 사용하도록 DatabaseProvider 인터페이스와 Koin 의존성 주입으로 연결을 만든다. DB 설정·환경변수·커넥션 풀·스키마 생성·로컬 H2 실행이 필요할 때 사용.
---

# 서버 데이터베이스 (Supabase / H2)

## 원칙

1. **DB 종류 선택은 `plugins/Database.kt`의 `databaseModule` 한 곳에서만** 한다.
2. Repository는 Exposed `Database`만 주입받고 **Exposed DSL만** 쓴다.
3. 벤더별 차이(드라이버, SSL, 풀 옵션)는 `DatabaseProvider` 구현 안에만 둔다.

```
application.yaml (database.provider)
        │
        ▼
databaseModule(config) ── single<DatabaseProvider> ── SupabaseDatabaseProvider  (기본, PostgreSQL)
        │                                         └─ H2DatabaseProvider        (로컬·테스트)
        ▼
single<Database> { get<DatabaseProvider>().connect() }
        │
        ▼
ExposedEventRepository(database: Database)
```

## 배치

```
server/src/main/kotlin/com/kotlincalendar/server/
├── common/database/
│   ├── DatabaseProvider.kt      # interface DatabaseProvider, JdbcSettings, HikariDatabaseProvider
│   ├── Providers.kt             # SupabaseDatabaseProvider, H2DatabaseProvider
│   └── DatabaseMigrator.kt      # enum DatabaseVendor(마이그레이션 위치), DatabaseMigrator(Flyway)
├── plugins/Database.kt          # databaseModule(config), databaseProviderFrom(config), configureDatabase()
server/src/main/resources/db/migration/
├── postgresql/                  # V1__…, V2__… (Supabase·PostgreSQL·H2)
└── supabase/                    # R__ 반복 마이그레이션만 (RLS 등 Supabase 전용)
```

## 인터페이스

```kotlin
interface DatabaseProvider : AutoCloseable {
    val vendor: DatabaseVendor            // 어떤 마이그레이션을 적용할지
    fun dataSource(): DataSource          // Flyway와 Exposed가 같은 풀을 쓴다
    fun connect(): Database = Database.connect(dataSource())
}

abstract class HikariDatabaseProvider(private val settings: JdbcSettings) : DatabaseProvider {
    protected abstract val driverClassName: String
    protected open fun HikariConfig.customize() = Unit
    // dataSource(): HikariDataSource를 한 번 만들어 재사용, close(): 풀 종료
}

class SupabaseDatabaseProvider(settings: JdbcSettings) : HikariDatabaseProvider(settings) {
    override val vendor = DatabaseVendor.SUPABASE
    override val driverClassName = "org.postgresql.Driver"
    override fun HikariConfig.customize() {
        addDataSourceProperty("sslmode", "require")
        addDataSourceProperty("prepareThreshold", "0") // transaction pooler(6543) 호환
    }
}

// H2DatabaseProvider: vendor = POSTGRESQL, URL에 MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
```

```kotlin
fun databaseModule(config: ApplicationConfig) = module {
    single<DatabaseProvider> { databaseProviderFrom(config) }
    single<Database> { get<DatabaseProvider>().connect() }
}

fun databaseProviderFrom(config: ApplicationConfig): DatabaseProvider =
    when (val provider = config.property("database.provider").getString().lowercase()) {
        "supabase" -> SupabaseDatabaseProvider(config.jdbcSettings("database.supabase"))
        "h2" -> H2DatabaseProvider(config.property("database.h2.url").getString())
        else -> error("Unknown database.provider '$provider'")
    }
```

`configureDatabase()`는 `DatabaseMigrator(provider.dataSource(), provider.vendor).migrate()`로 마이그레이션을 적용한 뒤 `Database`를 쓰고, `ApplicationStopped`에서 `provider.close()`로 풀을 닫는다. **`SchemaUtils.create`는 쓰지 않는다.**

## 설정 (`server/src/main/resources/application.yaml`)

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `DB_PROVIDER` | `supabase` | `supabase` \| `h2` |
| `SUPABASE_DB_URL` | (없음) | Supabase 대시보드 → Connect → **Session pooler** JDBC URL. 예: `jdbc:postgresql://aws-0-ap-northeast-2.pooler.supabase.com:5432/postgres` |
| `SUPABASE_DB_USER` | (없음) | 보통 `postgres.<project-ref>` |
| `SUPABASE_DB_PASSWORD` | (없음) | DB 비밀번호 |
| `SUPABASE_DB_POOL_SIZE` | `5` | Supabase 무료 플랜은 연결 수가 적다 |
| `H2_URL` | `jdbc:h2:mem:calendar;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1` | postgresql 마이그레이션을 그대로 적용하기 위해 PostgreSQL 모드 |

- 비밀번호·URL은 **절대 커밋하지 않는다.** 로컬은 셸 환경변수나 IDE Run Configuration, CI·배포는 시크릿으로 넣는다.
- Supabase 접속 정보 없이 로컬에서 띄우려면 `DB_PROVIDER=h2`.

## 스키마 규칙

- 시각: PostgreSQL `TIMESTAMPTZ` (또는 `TIMESTAMP WITH TIME ZONE`) / Exposed `timestamp("xxx_at")` (`kotlinx-datetime` `Instant`).
- 문자열: `VARCHAR(n)` 길이 명시, 해시처럼 고정 길이는 `CHAR(n)`.
- 테이블·컬럼 이름은 `snake_case` 복수형 테이블, SQL 예약어 피하기 (`end` → `end_at`).
- 제약 이름을 항상 붙인다: `pk_<table>`, `uq_<table>_<col>`, `fk_<table>_<ref>`, `ck_<table>_<규칙>`, `ix_<table>_<cols>`.
- 소유 관계는 `FOREIGN KEY … ON DELETE CASCADE` (사용자 삭제 시 그 사용자의 데이터 삭제).
- 비밀번호·토큰은 해시만 저장한다 (`password_hash`, `token_hash`).

## Flyway 마이그레이션

| 위치 | 대상 (`DatabaseVendor`) | 내용 |
|---|---|---|
| `db/migration/postgresql/` | SUPABASE, POSTGRESQL, H2 | 버전 마이그레이션 `V<n>__<설명>.sql` |
| `db/migration/supabase/` | SUPABASE | `R__` 반복 마이그레이션만 (예: `R__enable_row_level_security.sql`) |

규칙:
1. **이미 머지된 `V` 파일은 절대 수정하지 않는다.** 바꿀 게 있으면 새 버전(`V<n+1>__alter_…`)을 추가한다. Flyway checksum이 깨져 운영 DB가 기동하지 않는다.
2. 새 테이블을 만들면 `supabase/R__enable_row_level_security.sql`에 `ALTER TABLE <table> ENABLE ROW LEVEL SECURITY;`를 추가한다. Supabase는 public 스키마를 Data API로 노출하므로 RLS 없는 테이블은 anon 키로 읽힐 수 있다.
3. 마이그레이션은 H2(PostgreSQL 모드)와 실제 DB 양쪽에서 돌아가는 문법만 쓴다.
4. Exposed `Table` 객체는 마이그레이션과 컬럼 이름·타입을 맞춘다 (스키마를 만들지는 않음).
5. 데이터 변경(backfill)도 마이그레이션으로 하되, 큰 테이블은 배치로 나눈다.

## 테스트

- `databaseProviderFrom(MapApplicationConfig(...))`가 provider 값별로 올바른 구현을 고르는지 단위 테스트.
- 마이그레이션: `DatabaseMigratorTest`(H2 PostgreSQL 모드에서 적용·제약 조건), `MigrationParityTest`(디렉터리 규칙), `DatabaseMigrationIntegrationTest`(실제 PostgreSQL, `MIGRATION_TEST_POSTGRES_URL`이 있을 때만). 새 마이그레이션마다 제약 조건 테스트를 추가한다.
- Repository 테스트는 H2(PostgreSQL 모드)에 `DatabaseMigrator`로 마이그레이션을 적용한 뒤 실제 쿼리를 검증 (`ktor-mockk-testing`).
- 로컬에서 실제 DB 검증: `docker run -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16` 후 `MIGRATION_TEST_POSTGRES_URL="jdbc:postgresql://localhost:5432/postgres?user=postgres&password=postgres" ./gradlew :server:test --tests "*MigrationIntegrationTest"`.
- CI의 `migration-test` 잡이 PostgreSQL 16 서비스 컨테이너로 통합 테스트를 돌린다. Supabase에는 접속하지 않는다.
