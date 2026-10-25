package com.kotlincalendar.server.common.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

interface DatabaseProvider : AutoCloseable {
    fun connect(): Database
}

data class JdbcSettings(
    val url: String,
    val user: String = "",
    val password: String = "",
    val maxPoolSize: Int = 10,
)

abstract class HikariDatabaseProvider(
    private val settings: JdbcSettings,
) : DatabaseProvider {
    protected abstract val driverClassName: String
    private var dataSource: HikariDataSource? = null

    protected open fun HikariConfig.customize() = Unit

    override fun connect(): Database {
        val config =
            HikariConfig().apply {
                driverClassName = this@HikariDatabaseProvider.driverClassName
                jdbcUrl = settings.url
                username = settings.user
                password = settings.password
                maximumPoolSize = settings.maxPoolSize
                customize()
            }
        val ds = HikariDataSource(config)
        dataSource = ds
        return Database.connect(ds)
    }

    override fun close() {
        dataSource?.close()
    }
}
