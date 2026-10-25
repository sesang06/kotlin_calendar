package com.kotlincalendar.client.presentation.calendar

import com.kotlincalendar.client.domain.model.Event
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/** 일정 목록 한 줄에 필요한 표시용 데이터. */
data class CalendarEventItem(
    val id: String,
    val title: String,
    val timeRange: String,
)

fun Event.toCalendarEventItem(
    selectedDate: LocalDate,
    timeZone: TimeZone,
): CalendarEventItem =
    CalendarEventItem(
        id = id,
        title = title,
        timeRange = formatTimeRange(start.toLocalDateTime(timeZone), end.toLocalDateTime(timeZone), selectedDate),
    )

/**
 * `HH:mm - HH:mm` 형식. 시작·종료 시각이 [selectedDate]와 다른 날이면 앞에 `M/d`를 붙인다.
 */
fun formatTimeRange(
    start: LocalDateTime,
    end: LocalDateTime,
    selectedDate: LocalDate,
): String = "${formatPoint(start, selectedDate)} - ${formatPoint(end, selectedDate)}"

private fun formatPoint(
    dateTime: LocalDateTime,
    selectedDate: LocalDate,
): String {
    val time = "${dateTime.hour.pad2()}:${dateTime.minute.pad2()}"
    return if (dateTime.date == selectedDate) {
        time
    } else {
        "${dateTime.month.number}/${dateTime.day} $time"
    }
}

private fun Int.pad2(): String = toString().padStart(2, '0')
