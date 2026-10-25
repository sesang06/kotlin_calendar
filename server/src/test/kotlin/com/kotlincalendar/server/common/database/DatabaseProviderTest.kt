package com.kotlincalendar.server.common.database

import com.kotlincalendar.server.plugins.databaseProviderFrom
import io.ktor.server.config.MapApplicationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DatabaseProviderTest {
    @Test
    fun `provider가 supabase이면 SupabaseDatabaseProvider`() {
        val config =
            MapApplicationConfig(
                "database.provider" to "supabase",
                "database.supabase.url" to "jdbc:postgresql://localhost:5432/postgres",
                "database.supabase.user" to "user",
                "database.supabase.password" to "pass",
            )
        assertIs<SupabaseDatabaseProvider>(databaseProviderFrom(config))
    }

    @Test
    fun `provider가 h2이면 H2DatabaseProvider`() {
        val config =
            MapApplicationConfig(
                "database.provider" to "h2",
                "database.h2.url" to "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            )
        assertIs<H2DatabaseProvider>(databaseProviderFrom(config))
    }

    @Test
    fun `H2는 PostgreSQL 마이그레이션을, Supabase는 RLS까지 적용한다`() {
        assertEquals(DatabaseVendor.POSTGRESQL, H2DatabaseProvider("jdbc:h2:mem:x").vendor)
        assertEquals(DatabaseVendor.SUPABASE, SupabaseDatabaseProvider(JdbcSettings(url = "jdbc:postgresql://localhost/x")).vendor)
    }

    @Test
    fun `알 수 없는 provider면 시작 시 실패한다`() {
        assertFailsWith<IllegalStateException> {
            databaseProviderFrom(MapApplicationConfig("database.provider" to "mysql"))
        }
    }
}
