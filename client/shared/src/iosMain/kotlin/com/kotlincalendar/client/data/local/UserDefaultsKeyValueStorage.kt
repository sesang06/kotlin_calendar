package com.kotlincalendar.client.data.local

import platform.Foundation.NSUserDefaults

/** NSUserDefaults에 저장한다. Keychain보다 보호 수준이 낮아, 운영 배포 전에는 Keychain으로 바꾸는 것이 좋다. */
class UserDefaultsKeyValueStorage(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : KeyValueStorage {
    override fun get(key: String): String? = defaults.stringForKey(key)

    override fun put(
        key: String,
        value: String,
    ) {
        defaults.setObject(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}
