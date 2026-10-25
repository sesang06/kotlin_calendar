package com.kotlincalendar.server.common.database

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** 마이그레이션 디렉터리 규칙을 확인한다 (server-database 스킬). */
class MigrationParityTest {
    private val root = File("src/main/resources/db/migration")

    private fun versionedScripts(vendorDir: String): Set<String> =
        File(root, vendorDir)
            .listFiles()
            .orEmpty()
            .map { it.name }
            .filter { it.startsWith("V") && it.endsWith(".sql") }
            .toSortedSet()

    @Test
    fun `postgresql 디렉터리에 버전 마이그레이션이 존재한다`() {
        val postgresql = versionedScripts("postgresql")

        assertTrue(postgresql.isNotEmpty())
    }

    @Test
    fun `supabase 디렉터리에는 반복 마이그레이션만 둔다`() {
        val scripts = File(root, "supabase").listFiles().orEmpty().map { it.name }

        assertTrue(scripts.all { it.startsWith("R__") }, "versioned scripts in supabase/: $scripts")
    }
}
