package com.kotlincalendar.client.data.repository

import com.kotlincalendar.client.domain.model.AuthTokens
import com.kotlincalendar.client.domain.repository.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryTokenStorage : TokenStorage {
    private val _tokens = MutableStateFlow<AuthTokens?>(null)
    val tokens = _tokens.asStateFlow()

    override suspend fun getTokens(): AuthTokens? = _tokens.value

    override suspend fun saveTokens(tokens: AuthTokens) {
        _tokens.value = tokens
    }

    override suspend fun clearTokens() {
        _tokens.value = null
    }
}
