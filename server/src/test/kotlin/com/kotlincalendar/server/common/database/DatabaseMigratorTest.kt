package com.kotlincalendar.server.common.database

import org.h2.jdbcx.JdbcDataSource
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** H2 PostgreSQL 호환 모드로 마이그레이션을 검증한다. 실제 DB 검증은 [DatabaseMigrationIntegrationTest]. */
class DatabaseMigratorTest {
    private fun h2(): DataSource =
        JdbcDataSource().apply {
            setURL("jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
        }

    private fun migrated(): DataSource = h2().also { DatabaseMigrator(it, DatabaseVendor.POSTGRESQL).migrate() }

    @Test
    fun `마이그레이션은 users와 refresh_tokens 테이블을 만든다`() {
        val dataSource = h2()

        val result = DatabaseMigrator(dataSource, DatabaseVendor.POSTGRESQL).migrate()

        assertTrue(result.success)
        assertEquals(setOf("users", "refresh_tokens"), dataSource.userTables())
    }

    @Test
    fun `두 번 실행하면 두 번째에는 적용할 마이그레이션이 없다`() {
        val dataSource = h2()
        DatabaseMigrator(dataSource, DatabaseVendor.POSTGRESQL).migrate()

        val second = DatabaseMigrator(dataSource, DatabaseVendor.POSTGRESQL).migrate()

        assertEquals(0, second.migrationsExecuted)
    }

    @Test
    fun `같은 이메일로 두 번 가입할 수 없다`() {
        val dataSource = migrated()
        dataSource.insertUser(id = "u1", email = "a@example.com")

        assertFailsWith<SQLException> { dataSource.insertUser(id = "u2", email = "a@example.com") }
    }

    @Test
    fun `대문자가 섞인 이메일은 저장할 수 없다`() {
        val dataSource = migrated()

        assertFailsWith<SQLException> { dataSource.insertUser(id = "u1", email = "A@example.com") }
    }

    @Test
    fun `사용자를 지우면 그 사용자의 refresh token도 지워진다`() {
        val dataSource = migrated()
        dataSource.insertUser(id = "u1", email = "a@example.com")
        dataSource.execute(
            "INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at) " +
                "VALUES ('t1', 'u1', '${"a".repeat(64)}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        )

        dataSource.execute("DELETE FROM users WHERE id = 'u1'")

        assertEquals(0, dataSource.count("refresh_tokens"))
    }

    @Test
    fun `없는 사용자의 refresh token은 저장할 수 없다`() {
        val dataSource = migrated()

        assertFailsWith<SQLException> {
            dataSource.execute(
                "INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at) " +
                    "VALUES ('t1', 'nobody', '${"a".repeat(64)}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            )
        }
    }
}

internal fun DataSource.execute(sql: String) {
    connection.use { it.createStatement().use { statement -> statement.execute(sql) } }
}

internal fun DataSource.count(table: String): Int =
    connection.use {
        it.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $table").use { rs ->
                rs.next()
                rs.getInt(1)
            }
        }
    }

internal fun DataSource.insertUser(
    id: String,
    email: String,
) = execute(
    "INSERT INTO users (id, email, password_hash, display_name, created_at, updated_at) " +
        "VALUES ('$id', '$email', 'hash', 'name', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
)

/** 현재 catalog/schema 의 사용자 테이블 (시스템 카탈로그·Flyway 이력 제외). */
internal fun DataSource.userTables(): Set<String> =
    connection.use { connection ->
        connection.metaData.getTables(connection.catalog, connection.schema, "%", arrayOf("TABLE", "BASE TABLE")).use { rs ->
            buildSet {
                while (rs.next()) add(rs.getString("TABLE_NAME").lowercase())
            }
        } - "flyway_schema_history"
    }
