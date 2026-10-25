package com.kotlincalendar.client.presentation.event.create

import kotlin.time.Instant

// proto/calendar/v1/event.proto CreateEventRequest 제약과 맞춘다. 길이는 Unicode code point 기준.
const val EVENT_TITLE_MAX_LENGTH = 200
const val EVENT_DESCRIPTION_MAX_LENGTH = 2000

data class EventCreateValidation(
    val titleError: String? = null,
    val descriptionError: String? = null,
    val timeError: String? = null,
) {
    val isValid: Boolean get() = titleError == null && descriptionError == null && timeError == null
}

fun validateEventInput(
    title: String,
    description: String,
    start: Instant,
    end: Instant,
): EventCreateValidation =
    EventCreateValidation(
        titleError =
            when {
                title.isBlank() -> "제목을 입력해주세요."
                title.codePointLength() > EVENT_TITLE_MAX_LENGTH -> "제목은 ${EVENT_TITLE_MAX_LENGTH}자 이하로 입력해주세요."
                else -> null
            },
        descriptionError =
            if (description.codePointLength() > EVENT_DESCRIPTION_MAX_LENGTH) {
                "설명은 ${EVENT_DESCRIPTION_MAX_LENGTH}자 이하로 입력해주세요."
            } else {
                null
            },
        timeError = if (end <= start) "종료 시간은 시작 시간보다 늦어야 합니다." else null,
    )

/** UTF-16 서로게이트 쌍을 한 글자로 센 길이 (Unicode code point 수). */
fun String.codePointLength(): Int {
    var count = 0
    var i = 0
    while (i < length) {
        val isPair = this[i].isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()
        i += if (isPair) 2 else 1
        count++
    }
    return count
}
