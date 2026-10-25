package com.kotlincalendar.server.common.database

import org.jetbrains.exposed.sql.Database
import java.util.UUID

/** 운영과 같은 Flyway 마이그레이션을 적용한 테스트용 H2(PostgreSQL 모드) DB. */
internal fun migratedH2Database(): Database {
    val provider =
        H2DatabaseProvider(
            "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        )
    val dataSource = provider.dataSource()
    DatabaseMigrator(dataSource, provider.vendor).migrate()
    return Database.connect(dataSource)
}
