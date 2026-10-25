package com.kotlincalendar.client.presentation.event.detail

sealed interface EventDetailAction {
    data object Retry : EventDetailAction

    data object Back : EventDetailAction

    data object Edit : EventDetailAction

    data object RequestDelete : EventDetailAction

    data object ConfirmDelete : EventDetailAction

    data object CancelDelete : EventDetailAction

    data object DismissError : EventDetailAction
}
