package com.kotlincalendar.client.presentation.auth.signup

sealed interface SignUpAction {
    data class ChangeEmail(
        val email: String,
    ) : SignUpAction

    data class ChangePassword(
        val password: String,
    ) : SignUpAction

    data class ChangeDisplayName(
        val displayName: String,
    ) : SignUpAction

    data object Submit : SignUpAction

    data object NavigateToLogin : SignUpAction

    data object DismissError : SignUpAction
}
