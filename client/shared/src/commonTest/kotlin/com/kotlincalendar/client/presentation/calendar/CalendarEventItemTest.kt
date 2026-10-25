package com.kotlincalendar.client.presentation.calendar

import com.kotlincalendar.client.domain.model.Event
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarEventItemTest {
    private val date = LocalDate(2026, 10, 25)

    @Test
    fun formatTimeRange_sameDay_showsTimesOnly() {
        val result =
            formatTimeRange(
                start = LocalDateTime(2026, 10, 25, 9, 5),
                end = LocalDateTime(2026, 10, 25, 10, 30),
                selectedDate = date,
            )

        assertEquals("09:05 - 10:30", result)
    }

    @Test
    fun formatTimeRange_otherDay_prefixesMonthAndDay() {
        val result =
            formatTimeRange(
                start = LocalDateTime(2026, 10, 24, 23, 0),
                end = LocalDateTime(2026, 10, 26, 1, 0),
                selectedDate = date,
            )

        assertEquals("10/24 23:00 - 10/26 01:00", result)
    }

    @Test
    fun toCalendarEventItem_convertsToTimeZone() {
        val event =
            Event(
                id = "e1",
                title = "회의",
                start = LocalDateTime(2026, 10, 25, 14, 0).toInstant(TimeZone.UTC),
                end = LocalDateTime(2026, 10, 25, 15, 0).toInstant(TimeZone.UTC),
            )

        assertEquals(
            CalendarEventItem(id = "e1", title = "회의", timeRange = "14:00 - 15:00"),
            event.toCalendarEventItem(date, TimeZone.UTC),
        )
    }
}
