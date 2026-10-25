package com.kotlincalendar.client.presentation.profile

sealed interface ProfileAction {
    data object Back : ProfileAction

    data object Retry : ProfileAction

    data object Logout : ProfileAction
}
