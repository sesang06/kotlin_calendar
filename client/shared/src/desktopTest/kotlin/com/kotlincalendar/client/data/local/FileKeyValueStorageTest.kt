package com.kotlincalendar.client.data.local

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileKeyValueStorageTest {
    private val dir: File = Files.createTempDirectory("kv-test").toFile()
    private val file = File(dir, "nested/session.properties")

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun putThenGet_survivesNewInstance() {
        FileKeyValueStorage(file).put("k", "v")

        assertEquals("v", FileKeyValueStorage(file).get("k"))
        assertTrue(file.exists())
    }

    @Test
    fun remove_deletesKey() {
        val storage = FileKeyValueStorage(file)
        storage.put("k", "v")

        storage.remove("k")

        assertNull(FileKeyValueStorage(file).get("k"))
    }

    @Test
    fun missingFile_returnsNull() {
        assertNull(FileKeyValueStorage(file).get("k"))
    }

    @Test
    fun createdFile_isOwnerOnly() {
        // Windows 등 POSIX 권한이 없는 파일 시스템에서는 확인하지 않는다.
        if ("posix" !in FileSystems.getDefault().supportedFileAttributeViews()) return
        FileKeyValueStorage(file).put("k", "v")

        val perms = Files.getPosixFilePermissions(file.toPath()).map { it.name }.toSet()
        assertTrue(perms.none { it.startsWith("GROUP") || it.startsWith("OTHERS") }, "perms=$perms")
    }
}
