package com.kotlincalendar.client.presentation.calendar

import kotlinx.datetime.LocalDate

sealed interface CalendarAction {
    data object PreviousMonth : CalendarAction

    data object NextMonth : CalendarAction

    data object GoToToday : CalendarAction

    data class SelectDate(
        val date: LocalDate,
    ) : CalendarAction

    data class OpenEvent(
        val id: String,
    ) : CalendarAction

    data object CreateEvent : CalendarAction

    /** 선택된 날짜의 일정을 다시 불러온다 (일정 생성·삭제 후 등). */
    data object Refresh : CalendarAction

    data object Retry : CalendarAction
}
