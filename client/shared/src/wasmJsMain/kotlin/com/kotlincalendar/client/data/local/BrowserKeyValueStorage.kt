package com.kotlincalendar.client.data.local

import kotlinx.browser.window

/** 브라우저 localStorage. 비공개 모드 등으로 막혀 있으면 저장하지 않고 넘어간다. */
class BrowserKeyValueStorage : KeyValueStorage {
    override fun get(key: String): String? = runCatching { window.localStorage.getItem(key) }.getOrNull()

    override fun put(
        key: String,
        value: String,
    ) {
        runCatching { window.localStorage.setItem(key, value) }
    }

    override fun remove(key: String) {
        runCatching { window.localStorage.removeItem(key) }
    }
}
