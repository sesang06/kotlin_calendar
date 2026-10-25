package com.kotlincalendar.server.event.domain

import kotlin.time.Instant

data class CreateEventCommand(
    val title: String,
    val start: Instant,
    val end: Instant,
    val description: String = "",
    val ownerUserId: String = "",
)
