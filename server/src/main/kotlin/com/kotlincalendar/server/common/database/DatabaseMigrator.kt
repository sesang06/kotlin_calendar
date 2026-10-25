package com.kotlincalendar.server.common.database

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import javax.sql.DataSource

/**
 * DB 종류별 Flyway 마이그레이션 위치.
 */
enum class DatabaseVendor(
    val migrationLocations: List<String>,
) {
    /** Supabase: PostgreSQL 공통 마이그레이션 + Data API 차단(RLS). */
    SUPABASE(listOf("classpath:db/migration/postgresql", "classpath:db/migration/supabase")),
    POSTGRESQL(listOf("classpath:db/migration/postgresql")),
}

class DatabaseMigrator(
    private val dataSource: DataSource,
    private val vendor: DatabaseVendor,
) {
    fun migrate(): MigrateResult =
        Flyway
            .configure()
            .dataSource(dataSource)
            .locations(*vendor.migrationLocations.toTypedArray())
            .validateMigrationNaming(true)
            .load()
            .migrate()
}
