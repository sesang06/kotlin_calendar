package com.kotlincalendar.client.presentation.event.create

sealed interface EventCreateEffect {
    data class Created(
        val eventId: String,
    ) : EventCreateEffect

    data object NavigateBack : EventCreateEffect
}
