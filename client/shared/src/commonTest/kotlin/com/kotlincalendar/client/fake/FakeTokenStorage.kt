package com.kotlincalendar.client.fake

import com.kotlincalendar.client.domain.model.AuthTokens
import com.kotlincalendar.client.domain.repository.TokenStorage

class FakeTokenStorage(
    initialTokens: AuthTokens? = null,
) : TokenStorage {
    var tokens: AuthTokens? = initialTokens
    var getTokensCallCount = 0
    var saveTokensCallCount = 0
    var clearTokensCallCount = 0

    override suspend fun getTokens(): AuthTokens? {
        getTokensCallCount++
        return tokens
    }

    override suspend fun saveTokens(tokens: AuthTokens) {
        saveTokensCallCount++
        this.tokens = tokens
    }

    override suspend fun clearTokens() {
        clearTokensCallCount++
        tokens = null
    }
}
