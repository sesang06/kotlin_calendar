package com.kotlincalendar.client.presentation.event.create

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class EventCreateValidationTest {
    private val start = Instant.parse("2026-10-25T01:00:00Z")
    private val end = start + 1.hours

    @Test
    fun validInput_hasNoErrors() {
        val result = validateEventInput("회의", "", start, end)

        assertTrue(result.isValid)
    }

    @Test
    fun blankTitle_isError() {
        assertEquals("제목을 입력해주세요.", validateEventInput("", "", start, end).titleError)
        assertEquals("제목을 입력해주세요.", validateEventInput("   ", "", start, end).titleError)
    }

    @Test
    fun title200CodePoints_isValid_201_isError() {
        assertNull(validateEventInput("가".repeat(200), "", start, end).titleError)
        assertNotNull(validateEventInput("가".repeat(201), "", start, end).titleError)
    }

    @Test
    fun emojiTitle_countsCodePointsNotUtf16Units() {
        // 😀 는 UTF-16 2 unit, code point 1개. 200개면 length 400이지만 유효해야 한다.
        val emojiTitle = "😀".repeat(200)
        assertEquals(400, emojiTitle.length)
        assertEquals(200, emojiTitle.codePointLength())

        assertNull(validateEventInput(emojiTitle, "", start, end).titleError)
        assertNotNull(validateEventInput(emojiTitle + "😀", "", start, end).titleError)
    }

    @Test
    fun description2000CodePoints_isValid_2001_isError() {
        assertNull(validateEventInput("t", "😀".repeat(2000), start, end).descriptionError)
        assertEquals(
            "설명은 2000자 이하로 입력해주세요.",
            validateEventInput("t", "a".repeat(2001), start, end).descriptionError,
        )
    }

    @Test
    fun endNotAfterStart_isError() {
        assertNotNull(validateEventInput("t", "", start, start).timeError)
        assertNotNull(validateEventInput("t", "", end, start).timeError)
        assertFalse(validateEventInput("t", "", end, start).isValid)
    }

    @Test
    fun codePointLength_countsLoneSurrogateAsOne() {
        assertEquals(0, "".codePointLength())
        assertEquals(1, "\uD83D".codePointLength())
        assertEquals(3, "a😀b".codePointLength())
    }
}
