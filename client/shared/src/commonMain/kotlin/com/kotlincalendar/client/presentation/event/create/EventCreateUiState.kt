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
    /** 기존 일정을 수정하는 중인지. 화면 제목과 저장 동작이 달라진다. */
    val isEditing: Boolean = false,
    /** 수정할 일정을 불러오는 중. */
    val isLoadingEvent: Boolean = false,
    val errorMessage: String? = null,
)
