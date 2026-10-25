package com.kotlincalendar.client.presentation.calendar

import kotlinx.datetime.LocalDate

sealed interface CalendarEffect {
    data class NavigateToCreateEvent(
        val date: LocalDate,
    ) : CalendarEffect

    data class NavigateToEvent(
        val id: String,
    ) : CalendarEffect
}
