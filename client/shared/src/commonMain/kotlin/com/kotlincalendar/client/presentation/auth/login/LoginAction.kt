package com.kotlincalendar.client.presentation.auth.login

sealed interface LoginAction {
    data class ChangeEmail(
        val email: String,
    ) : LoginAction

    data class ChangePassword(
        val password: String,
    ) : LoginAction

    data object Submit : LoginAction

    data object NavigateToSignUp : LoginAction

    data object DismissError : LoginAction
}
