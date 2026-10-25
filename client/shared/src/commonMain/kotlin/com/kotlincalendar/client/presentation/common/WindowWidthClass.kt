package com.kotlincalendar.client.presentation.common

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 창 너비 구간. 화면 레이아웃을 너비에 따라 바꿀 때 기준으로 쓴다.
 */
enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded,
    ;

    companion object {
        val MediumMinWidth: Dp = 600.dp
        val ExpandedMinWidth: Dp = 840.dp

        fun fromWidth(width: Dp): WindowWidthClass =
            when {
                width < MediumMinWidth -> Compact
                width < ExpandedMinWidth -> Medium
                else -> Expanded
            }
    }
}
