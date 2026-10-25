package com.kotlincalendar.server.user.domain

import kotlin.time.Instant

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val createdAt: Instant,
)
