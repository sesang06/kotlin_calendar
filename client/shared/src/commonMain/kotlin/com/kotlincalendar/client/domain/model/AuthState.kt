package com.kotlincalendar.client.domain.model

sealed interface AuthState {
    data object Unauthenticated : AuthState

    data class Authenticated(
        val user: User,
    ) : AuthState
}
