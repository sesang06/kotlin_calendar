package com.kotlincalendar.server.common.database

import com.zaxxer.hikari.HikariConfig
import org.h2.jdbcx.JdbcDataSource
import javax.sql.DataSource

class SupabaseDatabaseProvider(
    settings: JdbcSettings,
) : HikariDatabaseProvider(settings) {
    override val vendor = DatabaseVendor.SUPABASE
    override val driverClassName = "org.postgresql.Driver"

    override fun HikariConfig.customize() {
        addDataSourceProperty("sslmode", "require")
        addDataSourceProperty("prepareThreshold", "0")
    }
}

/** H2는 PostgreSQL 호환 모드(`MODE=PostgreSQL`)로 PostgreSQL 마이그레이션을 그대로 적용한다. */
class H2DatabaseProvider(
    private val url: String,
) : DatabaseProvider {
    override val vendor = DatabaseVendor.POSTGRESQL

    override fun dataSource(): DataSource = JdbcDataSource().apply { setURL(this@H2DatabaseProvider.url) }

    override fun close() = Unit
}
