package com.kotlincalendar.client.domain.model

import kotlin.time.Instant

data class Event(
    val id: String,
    val title: String,
    val start: Instant,
    val end: Instant,
    val createdAt: Instant? = null,
    val description: String = "",
    val ownerUserId: String = "",
)
