package com.kotlincalendar.server.common.database

import com.kotlincalendar.server.plugins.databaseProviderFrom
import io.ktor.server.config.MapApplicationConfig
import kotlin.test.Test
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
    fun `provider가 mysql이면 MySqlDatabaseProvider`() {
        val config =
            MapApplicationConfig(
                "database.provider" to "mysql",
                "database.mysql.url" to "jdbc:mysql://localhost/x",
                "database.mysql.user" to "u",
                "database.mysql.password" to "p",
                "database.mysql.maxPoolSize" to "2",
            )
        assertIs<MySqlDatabaseProvider>(databaseProviderFrom(config))
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
}
