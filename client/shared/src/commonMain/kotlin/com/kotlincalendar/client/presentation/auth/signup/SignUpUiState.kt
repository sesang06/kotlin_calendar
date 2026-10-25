package com.kotlincalendar.client.presentation.auth.signup

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
