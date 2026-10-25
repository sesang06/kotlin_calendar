package com.kotlincalendar.client.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.ComposeViewport
import com.kotlincalendar.client.App
import com.kotlincalendar.client.di.initKoin
import com.kotlincalendar.client.web.resources.Res
import com.kotlincalendar.client.web.resources.noto_sans_kr_regular
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import kotlin.time.Duration.Companion.seconds

private val KOREAN_FONT_LOAD_TIMEOUT = 10.seconds

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    ComposeViewport(document.body!!) {
        KoreanFontGate {
            App()
        }
    }
}

/**
 * Wasm(Skia) 기본 폰트에는 한글 글리프가 없어 한글이 깨진다.
 * 한글 폰트를 폰트 폴백으로 등록한 뒤에 [content]를 그린다.
 * 로드에 실패하거나 시간이 초과되면 폰트 없이라도 [content]를 그린다.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun KoreanFontGate(content: @Composable () -> Unit) {
    val fontFamilyResolver = LocalFontFamilyResolver.current
    var fontReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            withTimeoutOrNull(KOREAN_FONT_LOAD_TIMEOUT) {
                val bytes = getFontResourceBytes(getSystemResourceEnvironment(), Res.font.noto_sans_kr_regular)
                fontFamilyResolver.preload(FontFamily(Font("NotoSansKR-Regular", bytes)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Failed to load Korean font: $e")
        }
        fontReady = true
    }

    if (fontReady) {
        content()
    }
}
