package com.kotlincalendar.server.event.domain

import kotlin.time.Instant

data class Event(
    val id: String,
    val title: String,
    val start: Instant,
    val end: Instant,
    val createdAt: Instant,
    val description: String = "",
    val ownerUserId: String = "",
)
