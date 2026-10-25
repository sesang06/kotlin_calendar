package com.kotlincalendar.server.plugins

import com.kotlincalendar.server.common.database.DatabaseProvider
import com.kotlincalendar.server.common.database.H2DatabaseProvider
import com.kotlincalendar.server.common.database.JdbcSettings
import com.kotlincalendar.server.common.database.MySqlDatabaseProvider
import com.kotlincalendar.server.common.database.SupabaseDatabaseProvider
import com.kotlincalendar.server.event.repository.EventTable
import com.kotlincalendar.server.user.repository.RefreshTokenTable
import com.kotlincalendar.server.user.repository.UserTable
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject

fun databaseModule(config: ApplicationConfig) =
    module {
        single<DatabaseProvider> { databaseProviderFrom(config) }
        single<Database> { get<DatabaseProvider>().connect() }
    }

fun databaseProviderFrom(config: ApplicationConfig): DatabaseProvider {
    val provider = config.propertyOrNull("database.provider")?.getString()?.lowercase() ?: "supabase"
    return when (provider) {
        "supabase" -> SupabaseDatabaseProvider(config.jdbcSettings("database.supabase"))
        "mysql" -> MySqlDatabaseProvider(config.jdbcSettings("database.mysql"))
        "h2" ->
            H2DatabaseProvider(
                config.propertyOrNull("database.h2.url")?.getString()
                    ?: "jdbc:h2:mem:calendar;DB_CLOSE_DELAY=-1",
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

fun Application.configureDatabase() {
    val database by inject<Database>()
    val provider by inject<DatabaseProvider>()

    transaction(database) {
        SchemaUtils.create(UserTable, RefreshTokenTable, EventTable)
    }

    monitor.subscribe(ApplicationStopped) {
        provider.close()
    }
}
