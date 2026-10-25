package com.kotlincalendar.server.common.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 실제 PostgreSQL에 마이그레이션을 적용한다.
 * 환경변수가 없으면 건너뛴다. CI의 migration-test 잡이 서비스 컨테이너로 실행한다.
 * URL 예: jdbc:postgresql://localhost:5432/calendar?user=postgres&password=postgres
 */
class DatabaseMigrationIntegrationTest {
    private fun dataSource(url: String) = HikariDataSource(HikariConfig().apply { jdbcUrl = url })

    @Test
    @EnabledIfEnvironmentVariable(named = "MIGRATION_TEST_POSTGRES_URL", matches = ".+")
    fun `PostgreSQL에 Supabase 마이그레이션(RLS 포함)을 적용한다`() {
        dataSource(System.getenv("MIGRATION_TEST_POSTGRES_URL")).use { dataSource ->
            val result = DatabaseMigrator(dataSource, DatabaseVendor.SUPABASE).migrate()

            assertTrue(result.success)
            assertTrue(dataSource.userTables().containsAll(setOf("users", "refresh_tokens", "events")))
        }
    }
}
