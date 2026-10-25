package com.kotlincalendar.client.presentation.event.detail

data class EventDetailUiState(
    val isLoading: Boolean = false,
    val loadErrorMessage: String? = null,
    val isLoaded: Boolean = false,
    val title: String = "",
    val timeRangeText: String = "",
    val description: String = "",
    val showDeleteConfirm: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteErrorMessage: String? = null,
)
