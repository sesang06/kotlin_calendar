package com.kotlincalendar.client.presentation.event.detail

sealed interface EventDetailEffect {
    data object NavigateBack : EventDetailEffect

    data object Deleted : EventDetailEffect
}
