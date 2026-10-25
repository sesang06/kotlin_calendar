package com.kotlincalendar.server.user.domain

data class SignUpCommand(
    val email: String,
    val password: String,
    val displayName: String,
)

data class LoginCommand(
    val email: String,
    val password: String,
)

data class RefreshTokenCommand(
    val refreshToken: String,
)

data class LogoutCommand(
    val refreshToken: String,
    val userId: String,
)

data class AuthResult(
    val user: User,
    val tokens: AuthTokens,
)
