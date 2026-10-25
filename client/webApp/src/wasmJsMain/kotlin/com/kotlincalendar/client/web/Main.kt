package com.kotlincalendar.client.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.kotlincalendar.client.App
import com.kotlincalendar.client.di.initKoin
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    ComposeViewport(document.body!!) { App() }
}
