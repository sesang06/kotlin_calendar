package com.kotlincalendar.client.fake

import kotlin.time.Clock
import kotlin.time.Instant

class FixedClock(
    private val instant: Instant,
) : Clock {
    override fun now(): Instant = instant
}
