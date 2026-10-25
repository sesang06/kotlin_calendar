package com.kotlincalendar.client.presentation.common

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowWidthClassTest {
    @Test
    fun fromWidth_below600dp_isCompact() {
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.fromWidth(0.dp))
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.fromWidth(360.dp))
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.fromWidth(599.dp))
    }

    @Test
    fun fromWidth_from600dpBelow840dp_isMedium() {
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidth(600.dp))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidth(839.dp))
    }

    @Test
    fun fromWidth_from840dp_isExpanded() {
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.fromWidth(840.dp))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.fromWidth(1920.dp))
    }
}
