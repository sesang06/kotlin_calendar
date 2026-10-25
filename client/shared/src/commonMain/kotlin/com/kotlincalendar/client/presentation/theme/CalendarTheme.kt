package com.kotlincalendar.client.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF4A5BD8),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDFE1FF),
        onPrimaryContainer = Color(0xFF0B1667),
        secondary = Color(0xFF5B5D72),
        secondaryContainer = Color(0xFFE0E0F9),
        onSecondaryContainer = Color(0xFF181A2C),
        tertiary = Color(0xFF00897B),
        background = Color(0xFFF8F8FC),
        onBackground = Color(0xFF1B1B21),
        surface = Color(0xFFF8F8FC),
        onSurface = Color(0xFF1B1B21),
        surfaceVariant = Color(0xFFE3E1EC),
        onSurfaceVariant = Color(0xFF46464F),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF2F2F8),
        surfaceContainer = Color(0xFFECECF3),
        outline = Color(0xFF777680),
        outlineVariant = Color(0xFFC7C5D0),
        error = Color(0xFFBA1A1A),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFBCC2FF),
        onPrimary = Color(0xFF1A2678),
        primaryContainer = Color(0xFF3241BF),
        onPrimaryContainer = Color(0xFFDFE1FF),
        secondary = Color(0xFFC4C5DD),
        secondaryContainer = Color(0xFF434659),
        onSecondaryContainer = Color(0xFFE0E0F9),
        tertiary = Color(0xFF4DD0C1),
        background = Color(0xFF121318),
        onBackground = Color(0xFFE4E1E9),
        surface = Color(0xFF121318),
        onSurface = Color(0xFFE4E1E9),
        surfaceVariant = Color(0xFF46464F),
        onSurfaceVariant = Color(0xFFC7C5D0),
        surfaceContainerLowest = Color(0xFF0D0E13),
        surfaceContainerLow = Color(0xFF1B1B21),
        surfaceContainer = Color(0xFF1F1F25),
        outline = Color(0xFF918F9A),
        outlineVariant = Color(0xFF46464F),
        error = Color(0xFFFFB4AB),
    )

private val CalendarShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

/** 일요일·토요일 표시 색 (라이트·다크 공통). */
object WeekendColors {
    val Sunday = Color(0xFFE5484D)
    val Saturday = Color(0xFF3E7BFA)
}

/** 시스템 다크 모드를 따르는 앱 테마. */
@Composable
fun CalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = CalendarShapes,
        content = content,
    )
}
