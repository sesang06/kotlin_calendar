package com.kotlincalendar.client.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kotlincalendar.client.App
import com.kotlincalendar.client.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kotlin Calendar",
        ) {
            App()
        }
    }
}
