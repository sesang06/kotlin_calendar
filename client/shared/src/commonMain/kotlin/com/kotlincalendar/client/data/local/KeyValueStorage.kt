package com.kotlincalendar.client.data.local

/** 플랫폼별 문자열 저장소. 웹은 localStorage, 그 외 플랫폼은 아직 메모리. */
interface KeyValueStorage {
    fun get(key: String): String?

    fun put(
        key: String,
        value: String,
    )

    fun remove(key: String)
}

class InMemoryKeyValueStorage : KeyValueStorage {
    private val values = mutableMapOf<String, String>()

    override fun get(key: String): String? = values[key]

    override fun put(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
