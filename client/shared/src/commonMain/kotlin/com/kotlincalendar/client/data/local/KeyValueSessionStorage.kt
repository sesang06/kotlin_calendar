package com.kotlincalendar.client.data.local

import com.kotlincalendar.client.domain.model.AuthTokens
import com.kotlincalendar.client.domain.model.User
import com.kotlincalendar.client.domain.repository.TokenStorage
import com.kotlincalendar.client.domain.repository.UserStorage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** 토큰과 사용자 정보를 [KeyValueStorage]에 JSON으로 저장한다. 읽을 수 없는 값은 지우고 없는 것으로 본다. */
class KeyValueSessionStorage(
    private val storage: KeyValueStorage,
) : TokenStorage,
    UserStorage {
    override suspend fun getTokens(): AuthTokens? = read(TOKENS_KEY, StoredTokens.serializer())?.toDomain()

    override suspend fun saveTokens(tokens: AuthTokens) {
        storage.put(TOKENS_KEY, json.encodeToString(StoredTokens.serializer(), StoredTokens.from(tokens)))
    }

    override suspend fun clearTokens() {
        storage.remove(TOKENS_KEY)
    }

    override suspend fun getUser(): User? = read(USER_KEY, StoredUser.serializer())?.toDomain()

    override suspend fun saveUser(user: User) {
        storage.put(USER_KEY, json.encodeToString(StoredUser.serializer(), StoredUser.from(user)))
    }

    override suspend fun clearUser() {
        storage.remove(USER_KEY)
    }

    private fun <T> read(
        key: String,
        serializer: KSerializer<T>,
    ): T? {
        val raw = storage.get(key) ?: return null
        return runCatching { json.decodeFromString(serializer, raw) }
            .onFailure { storage.remove(key) }
            .getOrNull()
    }

    @Serializable
    private data class StoredTokens(
        val accessToken: String,
        val accessTokenExpiresAt: String,
        val refreshToken: String,
        val refreshTokenExpiresAt: String,
    ) {
        fun toDomain() =
            AuthTokens(
                accessToken = accessToken,
                accessTokenExpiresAt = Instant.parse(accessTokenExpiresAt),
                refreshToken = refreshToken,
                refreshTokenExpiresAt = Instant.parse(refreshTokenExpiresAt),
            )

        companion object {
            fun from(tokens: AuthTokens) =
                StoredTokens(
                    accessToken = tokens.accessToken,
                    accessTokenExpiresAt = tokens.accessTokenExpiresAt.toString(),
                    refreshToken = tokens.refreshToken,
                    refreshTokenExpiresAt = tokens.refreshTokenExpiresAt.toString(),
                )
        }
    }

    @Serializable
    private data class StoredUser(
        val id: String,
        val email: String,
        val displayName: String,
        val createdAt: String,
    ) {
        fun toDomain() =
            User(
                id = id,
                email = email,
                displayName = displayName,
                createdAt = Instant.parse(createdAt),
            )

        companion object {
            fun from(user: User) =
                StoredUser(
                    id = user.id,
                    email = user.email,
                    displayName = user.displayName,
                    createdAt = user.createdAt.toString(),
                )
        }
    }

    private companion object {
        const val TOKENS_KEY = "kotlin_calendar.auth.tokens"
        const val USER_KEY = "kotlin_calendar.auth.user"
        val json = Json { ignoreUnknownKeys = true }
    }
}
