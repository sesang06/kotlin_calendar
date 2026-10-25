package com.kotlincalendar.server.plugins

import com.kotlincalendar.server.common.database.DatabaseMigrator
import com.kotlincalendar.server.common.database.DatabaseProvider
import com.kotlincalendar.server.common.database.H2DatabaseProvider
import com.kotlincalendar.server.common.database.JdbcSettings
import com.kotlincalendar.server.common.database.SupabaseDatabaseProvider
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import javax.sql.DataSource

fun databaseModule(config: ApplicationConfig) =
    module {
        single<DatabaseProvider> { databaseProviderFrom(config) }
        single<DataSource> { get<DatabaseProvider>().dataSource() }
        single<Database> { Database.connect(get<DataSource>()) }
    }

fun databaseProviderFrom(config: ApplicationConfig): DatabaseProvider {
    val provider = config.propertyOrNull("database.provider")?.getString()?.lowercase() ?: "supabase"
    return when (provider) {
        "supabase" -> SupabaseDatabaseProvider(config.jdbcSettings("database.supabase"))
        "h2" ->
            H2DatabaseProvider(
                config.propertyOrNull("database.h2.url")?.getString()
                    ?: "jdbc:h2:mem:calendar;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
            )
        else -> error("Unknown database.provider '$provider'")
    }
}

private fun ApplicationConfig.jdbcSettings(path: String): JdbcSettings {
    val url = propertyOrNull("$path.url")?.getString().orEmpty()
    val user = propertyOrNull("$path.user")?.getString().orEmpty()
    val password = propertyOrNull("$path.password")?.getString().orEmpty()
    val maxPoolSize = propertyOrNull("$path.maxPoolSize")?.getString()?.toIntOrNull() ?: 10
    return JdbcSettings(url = url, user = user, password = password, maxPoolSize = maxPoolSize)
}

/** 시작 시 Flyway 마이그레이션을 적용한다. 스키마는 `db/migration`에서만 바뀐다 (server-database 스킬). */
fun Application.configureDatabase() {
    val dataSource by inject<DataSource>()
    val provider by inject<DatabaseProvider>()

    DatabaseMigrator(dataSource, provider.vendor).migrate()

    monitor.subscribe(ApplicationStopped) {
        provider.close()
    }
}
