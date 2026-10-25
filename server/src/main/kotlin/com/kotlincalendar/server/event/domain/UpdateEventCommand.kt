package com.kotlincalendar.server.event.domain

import kotlin.time.Instant

/** 일정 전체 수정. [id]·[ownerUserId]로 대상을 찾고 나머지 필드로 바꾼다. */
data class UpdateEventCommand(
    val id: String,
    val ownerUserId: String,
    val title: String,
    val start: Instant,
    val end: Instant,
    val description: String = "",
)
