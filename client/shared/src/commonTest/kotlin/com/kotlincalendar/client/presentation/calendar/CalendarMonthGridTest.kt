package com.kotlincalendar.client.presentation.calendar

import com.kotlincalendar.client.domain.model.Event
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class CalendarMonthGridTest {
    @Test
    fun firstDayOfMonth_returnsDayOne() {
        assertEquals(LocalDate(2026, 10, 1), firstDayOfMonth(LocalDate(2026, 10, 25)))
    }

    @Test
    fun grid_october2026_startsOnSundayBeforeFirstAndHasFiveWeeks() {
        // 2026-10-01은 목요일.
        val weeks = buildMonthGrid(LocalDate(2026, 10, 25))

        assertEquals(5, weeks.size)
        weeks.forEach { assertEquals(DAYS_IN_WEEK, it.size) }
        assertEquals(CalendarDay(LocalDate(2026, 9, 27), isInCurrentMonth = false), weeks.first().first())
        assertEquals(CalendarDay(LocalDate(2026, 10, 1), isInCurrentMonth = true), weeks.first()[4])
        assertEquals(CalendarDay(LocalDate(2026, 10, 31), isInCurrentMonth = true), weeks.last().last())
    }

    @Test
    fun grid_everyWeekStartsOnSunday() {
        val weeks = buildMonthGrid(LocalDate(2026, 3, 1))

        weeks.forEach { week ->
            assertEquals(DayOfWeek.SUNDAY, week.first().date.dayOfWeek)
            assertEquals(DayOfWeek.SATURDAY, week.last().date.dayOfWeek)
        }
    }

    @Test
    fun grid_monthStartingOnSunday_hasNoLeadingDays() {
        // 2026-02-01은 일요일, 2월은 28일 → 정확히 4주.
        val weeks = buildMonthGrid(LocalDate(2026, 2, 10))

        assertEquals(4, weeks.size)
        assertTrue(weeks.flatten().all { it.isInCurrentMonth })
        assertEquals(LocalDate(2026, 2, 1), weeks.first().first().date)
        assertEquals(LocalDate(2026, 2, 28), weeks.last().last().date)
    }

    @Test
    fun grid_longMonth_hasSixWeeksWithTrailingDays() {
        // 2026-08-01은 토요일, 31일 → 6주.
        val weeks = buildMonthGrid(LocalDate(2026, 8, 1))

        assertEquals(6, weeks.size)
        assertEquals(LocalDate(2026, 7, 26), weeks.first().first().date)
        assertEquals(LocalDate(2026, 9, 5), weeks.last().last().date)
        assertFalse(weeks.last().last().isInCurrentMonth)
    }

    @Test
    fun grid_containsEveryDayOfMonthExactlyOnce() {
        val days = buildMonthGrid(LocalDate(2024, 2, 1)).flatten().filter { it.isInCurrentMonth }

        assertEquals((1..29).map { LocalDate(2024, 2, it) }, days.map { it.date })
    }

    @Test
    fun monthGridRange_october2026_coversPaddedWeeks() {
        val range = monthGridRange(LocalDate(2026, 10, 1))

        assertEquals(LocalDate(2026, 9, 27), range.start)
        assertEquals(LocalDate(2026, 10, 31), range.endInclusive)
    }

    @Test
    fun eventDays_multiDayEvent_marksEachDayButNotEndAtMidnight() {
        val event =
            Event(
                id = "e",
                title = "여행",
                start = Instant.parse("2026-10-10T09:00:00Z"),
                end = Instant.parse("2026-10-13T00:00:00Z"),
            )

        val days = eventDays(listOf(event), TimeZone.UTC, LocalDate(2026, 9, 27)..LocalDate(2026, 10, 31))

        assertEquals(setOf(LocalDate(2026, 10, 10), LocalDate(2026, 10, 11), LocalDate(2026, 10, 12)), days)
    }

    @Test
    fun eventDays_clipsToRange() {
        val event =
            Event(
                id = "e",
                title = "긴 일정",
                start = Instant.parse("2026-09-20T00:00:00Z"),
                end = Instant.parse("2026-09-29T00:00:00Z"),
            )

        val days = eventDays(listOf(event), TimeZone.UTC, LocalDate(2026, 9, 27)..LocalDate(2026, 10, 31))

        assertEquals(setOf(LocalDate(2026, 9, 27), LocalDate(2026, 9, 28)), days)
    }
}
