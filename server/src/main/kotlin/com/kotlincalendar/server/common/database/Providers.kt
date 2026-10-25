package com.kotlincalendar.server.common.database

import com.zaxxer.hikari.HikariConfig
import org.jetbrains.exposed.sql.Database

class SupabaseDatabaseProvider(
    settings: JdbcSettings,
) : HikariDatabaseProvider(settings) {
    override val driverClassName = "org.postgresql.Driver"

    override fun HikariConfig.customize() {
        addDataSourceProperty("sslmode", "require")
        addDataSourceProperty("prepareThreshold", "0")
    }
}

class MySqlDatabaseProvider(
    settings: JdbcSettings,
) : HikariDatabaseProvider(settings) {
    override val driverClassName = "com.mysql.cj.jdbc.Driver"
}

class H2DatabaseProvider(
    private val url: String,
) : DatabaseProvider {
    override fun connect(): Database =
        Database.connect(
            url = url,
            driver = "org.h2.Driver",
        )

    override fun close() = Unit
}
