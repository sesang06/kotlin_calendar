package com.kotlincalendar.client.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import com.kotlincalendar.client.data.remote.ApiConfig
import com.kotlincalendar.client.data.remote.Transport
import com.kotlincalendar.client.di.initKoin
import kotlinx.browser.document
import org.koin.dsl.module

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin {
        modules(
            module {
                single {
                    ApiConfig(
                        httpBaseUrl = "",
                        grpcBaseUrl = "",
                        transport = Transport.HTTP,
                    )
                }
            },
        )
    }

    val body = document.body ?: return
    ComposeViewport(body) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Kotlin Calendar Web",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Compose Multiplatform Web (Wasm)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
