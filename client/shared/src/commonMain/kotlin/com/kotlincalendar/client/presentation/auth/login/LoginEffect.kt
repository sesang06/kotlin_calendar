package com.kotlincalendar.client.presentation.auth.login

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect

    data object NavigateToSignUp : LoginEffect

    data class ShowSnackbar(
        val message: String,
    ) : LoginEffect
}
