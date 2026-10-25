---
name: server-database
description: 서버 DB 연결(Supabase/H2)과 Flyway 마이그레이션·스키마 규칙. DatabaseProvider 주입, 환경변수, 마이그레이션 추가, RLS. DB 설정이나 db/migration을 건드릴 때 읽는다.
---

# 서버 데이터베이스

- DB 선택은 `plugins/Database.kt`의 `databaseModule`/`databaseProviderFrom` **한 곳**에서만. 벤더 차이는 `common/database/Providers.kt`의 `DatabaseProvider` 구현 안에만.
- `configureDatabase()`가 `DatabaseMigrator`(Flyway)로 마이그레이션을 적용한다. Repository는 주입된 `Database`만 안다.

## 환경변수 (`application.yaml`은 `$ENV:default`만, 비밀값 커밋 금지)

`DB_PROVIDER`(`supabase`|`h2`, 기본 supabase), `SUPABASE_DB_URL`(Session pooler JDBC URL), `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD`, `SUPABASE_DB_POOL_SIZE`(5), `H2_URL`(PostgreSQL 모드). 로컬은 `DB_PROVIDER=h2`.

## 마이그레이션 (`server/src/main/resources/db/migration/`)

- `postgresql/V<n>__<설명>.sql`: 모든 벤더(Supabase·PostgreSQL·H2) 공통. `supabase/R__*.sql`: Supabase 전용 반복 마이그레이션.
- **머지된 V 파일은 절대 수정하지 않는다** → 새 `V<n+1>__alter_…`.
- 새 테이블은 `supabase/R__enable_row_level_security.sql`에 `ENABLE ROW LEVEL SECURITY` 추가.
- H2(PostgreSQL 모드)와 PostgreSQL 양쪽에서 돌아가는 문법만.

## 스키마 규칙

- 시각 `TIMESTAMPTZ` / Exposed `timestamp("xxx_at")`, 문자열 `VARCHAR(n)` 길이 명시, 테이블은 snake_case 복수형, 예약어 회피(`end` → `end_at`).
- 제약 이름: `pk_<t>`, `uq_<t>_<col>`, `fk_<t>_<ref>`, `ck_<t>_<규칙>`, `ix_<t>_<cols>`. 소유 관계는 `ON DELETE CASCADE`.
- 비밀번호·토큰은 해시만 (`password_hash`, `token_hash`).

## 테스트

새 마이그레이션마다 `DatabaseMigratorTest`에 제약 조건 테스트. 실제 PostgreSQL 검증(`DatabaseMigrationIntegrationTest`)은 `MIGRATION_TEST_POSTGRES_URL`이 있을 때만 돈다 (CI `migration-test` 잡). Supabase에는 접속하지 않는다.
