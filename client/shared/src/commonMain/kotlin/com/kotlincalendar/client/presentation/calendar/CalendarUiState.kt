package com.kotlincalendar.client.presentation.calendar

import kotlinx.datetime.LocalDate

data class CalendarUiState(
    val today: LocalDate,
    val selectedDate: LocalDate = today,
    /** 화면에 보이는 달의 1일. */
    val visibleMonth: LocalDate = firstDayOfMonth(selectedDate),
    val events: List<CalendarEventItem> = emptyList(),
    /** 보이는 달 그리드에서 일정이 있는 날짜. 날짜 칸에 점으로 표시한다. */
    val eventDays: Set<LocalDate> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
