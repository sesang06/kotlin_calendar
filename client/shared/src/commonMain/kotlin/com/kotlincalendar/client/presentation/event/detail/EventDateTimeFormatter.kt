package com.kotlincalendar.client.presentation.event.detail

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val koreanDayOfWeekNames =
    DayOfWeekNames(listOf("월", "화", "수", "목", "금", "토", "일"))

private val koreanDateTimeFormat =
    LocalDateTime.Format {
        year()
        chars("년 ")
        monthNumber(Padding.NONE)
        chars("월 ")
        day(Padding.NONE)
        chars("일 (")
        dayOfWeek(koreanDayOfWeekNames)
        chars(") ")
        hour()
        char(':')
        minute()
    }

private val timeOnlyFormat =
    LocalDateTime.Format {
        hour()
        char(':')
        minute()
    }

/** 예: `2026년 10월 25일 (일) 14:00` */
fun formatEventDateTime(
    instant: Instant,
    timeZone: TimeZone,
): String = instant.toLocalDateTime(timeZone).format(koreanDateTimeFormat)

/**
 * 시작–종료 범위. 같은 날이면 종료는 시각만 표시한다.
 * 예: `2026년 10월 25일 (일) 14:00 – 15:30`
 */
fun formatEventRange(
    start: Instant,
    end: Instant,
    timeZone: TimeZone,
): String {
    val startLocal = start.toLocalDateTime(timeZone)
    val endLocal = end.toLocalDateTime(timeZone)
    val endText =
        if (startLocal.date == endLocal.date) {
            endLocal.format(timeOnlyFormat)
        } else {
            endLocal.format(koreanDateTimeFormat)
        }
    return "${startLocal.format(koreanDateTimeFormat)} – $endText"
}
