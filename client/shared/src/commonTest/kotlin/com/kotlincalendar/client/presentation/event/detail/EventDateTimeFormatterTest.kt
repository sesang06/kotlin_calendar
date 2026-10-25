package com.kotlincalendar.client.presentation.event.detail

import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class EventDateTimeFormatterTest {
    private val seoul = UtcOffset(hours = 9).asTimeZone()

    @Test
    fun formatEventDateTime_usesKoreanStyleInGivenTimeZone() {
        val instant = Instant.parse("2026-10-25T05:00:00Z")

        assertEquals("2026년 10월 25일 (일) 14:00", formatEventDateTime(instant, seoul))
    }

    @Test
    fun formatEventDateTime_doesNotPadMonthAndDayButPadsTime() {
        val instant = Instant.parse("2026-03-01T23:05:00Z")

        assertEquals("2026년 3월 2일 (월) 08:05", formatEventDateTime(instant, seoul))
    }

    @Test
    fun formatEventRange_sameDay_showsOnlyEndTime() {
        val start = Instant.parse("2026-10-25T05:00:00Z")
        val end = Instant.parse("2026-10-25T06:30:00Z")

        assertEquals("2026년 10월 25일 (일) 14:00 – 15:30", formatEventRange(start, end, seoul))
    }

    @Test
    fun formatEventRange_differentDays_showsFullEnd() {
        val start = Instant.parse("2026-10-25T14:00:00Z")
        val end = Instant.parse("2026-10-25T16:00:00Z")

        assertEquals(
            "2026년 10월 25일 (일) 23:00 – 2026년 10월 26일 (월) 01:00",
            formatEventRange(start, end, seoul),
        )
    }
}
