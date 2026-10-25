package com.kotlincalendar.client.presentation.calendar

import kotlinx.datetime.LocalDate

data class CalendarUiState(
    val today: LocalDate,
    val selectedDate: LocalDate = today,
    /** 화면에 보이는 달의 1일. */
    val visibleMonth: LocalDate = firstDayOfMonth(selectedDate),
    val events: List<CalendarEventItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
