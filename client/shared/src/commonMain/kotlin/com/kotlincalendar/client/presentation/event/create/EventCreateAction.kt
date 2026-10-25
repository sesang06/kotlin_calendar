package com.kotlincalendar.client.presentation.event.create

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed interface EventCreateAction {
    data class ChangeTitle(
        val title: String,
    ) : EventCreateAction

    data class ChangeDescription(
        val description: String,
    ) : EventCreateAction

    data class ChangeStartDate(
        val date: LocalDate,
    ) : EventCreateAction

    data class ChangeStartTime(
        val time: LocalTime,
    ) : EventCreateAction

    data class ChangeEndDate(
        val date: LocalDate,
    ) : EventCreateAction

    data class ChangeEndTime(
        val time: LocalTime,
    ) : EventCreateAction

    data object Save : EventCreateAction

    data object Back : EventCreateAction

    data object DismissError : EventCreateAction
}
