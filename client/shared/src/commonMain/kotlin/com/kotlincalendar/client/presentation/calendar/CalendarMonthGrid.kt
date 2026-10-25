package com.kotlincalendar.client.presentation.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

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
