package com.kotlincalendar.client.presentation.profile

sealed interface ProfileEffect {
    data object NavigateBack : ProfileEffect
}
