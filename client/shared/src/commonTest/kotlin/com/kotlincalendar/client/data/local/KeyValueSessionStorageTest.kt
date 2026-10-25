package com.kotlincalendar.client.data.local

import com.kotlincalendar.client.domain.model.AuthTokens
import com.kotlincalendar.client.domain.model.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class KeyValueSessionStorageTest {
    private val tokens =
        AuthTokens(
            accessToken = "access-123",
            accessTokenExpiresAt = Instant.parse("2026-10-25T00:15:00.123456789Z"),
            refreshToken = "refresh-456",
            refreshTokenExpiresAt = Instant.parse("2026-11-25T00:00:00Z"),
        )

    private val user =
        User(
            id = "user-1",
            email = "test@example.com",
            displayName = "Test User",
            createdAt = Instant.parse("2026-10-01T00:00:00Z"),
        )

    @Test
    fun savedSession_survivesNewStorageInstance() =
        runTest {
            val backing = InMemoryKeyValueStorage()
            KeyValueSessionStorage(backing).apply {
                saveTokens(tokens)
                saveUser(user)
            }

            val reloaded = KeyValueSessionStorage(backing)

            assertEquals(tokens, reloaded.getTokens())
            assertEquals(user, reloaded.getUser())
        }

    @Test
    fun clear_removesValues() =
        runTest {
            val storage = KeyValueSessionStorage(InMemoryKeyValueStorage())
            storage.saveTokens(tokens)
            storage.saveUser(user)

            storage.clearTokens()
            storage.clearUser()

            assertNull(storage.getTokens())
            assertNull(storage.getUser())
        }

    @Test
    fun corruptedValue_isTreatedAsMissingAndRemoved() =
        runTest {
            val backing = InMemoryKeyValueStorage()
            backing.put("kotlin_calendar.auth.tokens", "not-json")

            assertNull(KeyValueSessionStorage(backing).getTokens())
            assertNull(backing.get("kotlin_calendar.auth.tokens"))
        }
}
