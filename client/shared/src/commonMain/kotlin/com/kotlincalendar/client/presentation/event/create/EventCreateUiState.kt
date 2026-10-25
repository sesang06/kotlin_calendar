package com.kotlincalendar.client.presentation.event.create

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class EventCreateUiState(
    val title: String = "",
    val description: String = "",
    val startDate: LocalDate,
    val startTime: LocalTime,
    val endDate: LocalDate,
    val endTime: LocalTime,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val timeError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)
