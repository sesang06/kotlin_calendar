package com.kotlincalendar.client.web

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.ComposeViewport
import com.kotlincalendar.client.App
import com.kotlincalendar.client.di.initKoin
import com.kotlincalendar.client.web.resources.Res
import com.kotlincalendar.client.web.resources.noto_sans_kr_regular
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    initKoin()
    ComposeViewport(document.body!!) {
        // Wasm(Skia) 기본 폰트에는 한글 글리프가 없어 한글이 깨진다.
        // 한글 폰트를 폰트 폴백으로 등록한 뒤에 화면을 그린다.
        val koreanFont by preloadFont(Res.font.noto_sans_kr_regular)
        val fontFamilyResolver = LocalFontFamilyResolver.current
        var fontReady by remember { mutableStateOf(false) }

        LaunchedEffect(koreanFont) {
            val font = koreanFont ?: return@LaunchedEffect
            fontFamilyResolver.preload(FontFamily(font))
            fontReady = true
        }

        if (fontReady) {
            App()
        }
    }
}
