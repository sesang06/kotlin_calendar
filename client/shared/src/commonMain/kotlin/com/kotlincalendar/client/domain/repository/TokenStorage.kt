package com.kotlincalendar.client.domain.repository

import com.kotlincalendar.client.domain.model.AuthTokens

interface TokenStorage {
    suspend fun getTokens(): AuthTokens?

    suspend fun saveTokens(tokens: AuthTokens)

    suspend fun clearTokens()
}
