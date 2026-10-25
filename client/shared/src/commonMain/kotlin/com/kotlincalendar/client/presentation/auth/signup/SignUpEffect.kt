package com.kotlincalendar.client.presentation.auth.signup

sealed interface SignUpEffect {
    data object NavigateToHome : SignUpEffect

    data object NavigateToLogin : SignUpEffect

    data class ShowSnackbar(
        val message: String,
    ) : SignUpEffect
}
