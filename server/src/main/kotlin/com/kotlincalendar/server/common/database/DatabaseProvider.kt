package com.kotlincalendar.server.common.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

/** DB 벤더별 접속. Flyway([DatabaseMigrator])와 Exposed가 같은 [DataSource]를 쓴다. */
interface DatabaseProvider : AutoCloseable {
    val vendor: DatabaseVendor

    fun dataSource(): DataSource
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

    override fun dataSource(): DataSource {
        val config =
            HikariConfig().apply {
                driverClassName = this@HikariDatabaseProvider.driverClassName
                jdbcUrl = settings.url
                username = settings.user
                password = settings.password
                maximumPoolSize = settings.maxPoolSize
                customize()
            }
        return HikariDataSource(config).also { dataSource = it }
    }

    override fun close() {
        dataSource?.close()
    }
}
