package com.kotlincalendar.client.presentation.profile

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    /** 가입일. 예: `2026년 10월 1일 (목)` */
    val joinedAt: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggingOut: Boolean = false,
)
