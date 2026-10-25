package com.kotlincalendar.client.presentation.calendar

import com.kotlincalendar.client.domain.model.Event
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.nanoseconds

/** 달력 그리드는 일요일부터 시작한다. */
val CalendarWeekStart: DayOfWeek = DayOfWeek.SUNDAY

const val DAYS_IN_WEEK = 7

data class CalendarDay(
    val date: LocalDate,
    val isInCurrentMonth: Boolean,
)

/** [date]가 속한 달의 1일. */
fun firstDayOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month, 1)

/**
 * [month]가 속한 달의 달력 그리드를 주 단위로 만든다.
 *
 * 각 주는 일요일부터 토요일까지 7일이며, 앞뒤 빈칸은 이전·다음 달 날짜로 채운다 ([CalendarDay.isInCurrentMonth] = false).
 * 주의 개수는 달에 따라 4~6개다.
 */
fun buildMonthGrid(month: LocalDate): List<List<CalendarDay>> {
    val first = firstDayOfMonth(month)
    val nextMonthFirst = first.plus(1, DateTimeUnit.MONTH)
    val leading = (first.dayOfWeek.isoDayNumber - CalendarWeekStart.isoDayNumber + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val gridStart = first.minus(leading, DateTimeUnit.DAY)

    val weeks = mutableListOf<List<CalendarDay>>()
    var weekStart = gridStart
    while (weekStart < nextMonthFirst) {
        weeks +=
            (0 until DAYS_IN_WEEK).map { offset ->
                val date = weekStart.plus(offset, DateTimeUnit.DAY)
                CalendarDay(date = date, isInCurrentMonth = date.year == first.year && date.month == first.month)
            }
        weekStart = weekStart.plus(DAYS_IN_WEEK, DateTimeUnit.DAY)
    }
    return weeks
}

/** 그리드의 첫날과 마지막 날 (이전·다음 달 날짜 포함). */
fun monthGridRange(month: LocalDate): ClosedRange<LocalDate> {
    val weeks = buildMonthGrid(month)
    return weeks.first().first().date..weeks.last().last().date
}

/**
 * [events]가 걸쳐 있는 날짜 중 [range] 안의 날짜.
 * 종료 시각은 포함하지 않으므로 자정에 끝나는 일정은 다음 날에 표시하지 않는다.
 */
fun eventDays(
    events: List<Event>,
    timeZone: TimeZone,
    range: ClosedRange<LocalDate>,
): Set<LocalDate> {
    val days = mutableSetOf<LocalDate>()
    for (event in events) {
        val startDate = event.start.toLocalDateTime(timeZone).date
        val lastInstant = if (event.end > event.start) event.end - 1.nanoseconds else event.start
        val endDate = lastInstant.toLocalDateTime(timeZone).date
        var date = maxOf(startDate, range.start)
        val last = minOf(endDate, range.endInclusive)
        while (date <= last) {
            days += date
            date = date.plus(1, DateTimeUnit.DAY)
        }
    }
    return days
}

/** [date]가 속한 주(일요일 시작)의 첫날~마지막 날. */
fun weekRange(date: LocalDate): ClosedRange<LocalDate> {
    val offset = (date.dayOfWeek.isoDayNumber - CalendarWeekStart.isoDayNumber + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val start = date.minus(offset, DateTimeUnit.DAY)
    return start..start.plus(DAYS_IN_WEEK - 1, DateTimeUnit.DAY)
}

/** [range]의 날마다 그날과 겹치는 일정 (시작 시각 순). */
fun groupEventsByDay(
    events: List<Event>,
    timeZone: TimeZone,
    range: ClosedRange<LocalDate>,
): List<CalendarWeekDay> {
    val sorted = events.sortedBy { it.start }
    val days = mutableListOf<CalendarWeekDay>()
    var date = range.start
    while (date <= range.endInclusive) {
        val day = date
        val items =
            sorted
                .filter { day in eventDays(listOf(it), timeZone, day..day) }
                .map { it.toCalendarEventItem(day, timeZone) }
        days += CalendarWeekDay(day, items)
        date = date.plus(1, DateTimeUnit.DAY)
    }
    return days
}
