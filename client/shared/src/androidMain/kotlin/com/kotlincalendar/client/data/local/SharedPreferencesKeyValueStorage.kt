package com.kotlincalendar.client.data.local

import android.content.Context

/** 앱 전용 SharedPreferences. 다른 앱은 읽을 수 없지만 암호화하지는 않는다. */
class SharedPreferencesKeyValueStorage(
    context: Context,
) : KeyValueStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun get(key: String): String? = prefs.getString(key, null)

    override fun put(
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    private companion object {
        const val PREFS_NAME = "kotlin_calendar_session"
    }
}
