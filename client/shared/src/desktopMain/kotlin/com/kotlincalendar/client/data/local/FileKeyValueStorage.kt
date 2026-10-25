package com.kotlincalendar.client.data.local

import java.io.File
import java.util.Properties

/**
 * 사용자 홈의 properties 파일에 저장한다 (기본 `~/.kotlin_calendar/session.properties`).
 * 파일은 소유자만 읽고 쓸 수 있게 권한을 좁히지만 암호화하지는 않는다.
 */
class FileKeyValueStorage(
    private val file: File = File(System.getProperty("user.home"), ".kotlin_calendar/session.properties"),
) : KeyValueStorage {
    private val lock = Any()

    override fun get(key: String): String? = synchronized(lock) { load().getProperty(key) }

    override fun put(
        key: String,
        value: String,
    ) = synchronized(lock) {
        val props = load()
        props.setProperty(key, value)
        save(props)
    }

    override fun remove(key: String) =
        synchronized(lock) {
            val props = load()
            if (props.remove(key) != null) save(props)
        }

    private fun load(): Properties =
        Properties().apply {
            if (file.exists()) runCatching { file.inputStream().use { load(it) } }
        }

    private fun save(props: Properties) {
        runCatching {
            file.parentFile?.mkdirs()
            if (!file.exists()) {
                file.createNewFile()
                file.setReadable(false, false)
                file.setWritable(false, false)
                file.setReadable(true, true)
                file.setWritable(true, true)
            }
            file.outputStream().use { props.store(it, null) }
        }
    }
}
