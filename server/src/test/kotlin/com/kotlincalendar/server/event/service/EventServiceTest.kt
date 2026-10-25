package com.kotlincalendar.server.event.service

import com.kotlincalendar.server.common.error.NotFoundException
import com.kotlincalendar.server.common.error.ValidationException
import com.kotlincalendar.server.event.domain.CreateEventCommand
import com.kotlincalendar.server.event.domain.Event
import com.kotlincalendar.server.event.repository.EventRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

class EventServiceTest {
    private val repo = mockk<EventRepository>()
    private val now = Instant.parse("2026-10-25T00:00:00Z")
    private val service =
        EventService(
            eventRepository = repo,
            clock =
                object : Clock {
                    override fun now() = now
                },
            idGenerator = { "id-1" },
        )

    private val command =
        CreateEventCommand(
            title = "회의",
            start = Instant.parse("2026-10-26T01:00:00Z"),
            end = Instant.parse("2026-10-26T02:00:00Z"),
            description = "설명",
            ownerUserId = "user-1",
        )

    private val event =
        Event(
            id = "id-1",
            title = "회의",
            start = Instant.parse("2026-10-26T01:00:00Z"),
            end = Instant.parse("2026-10-26T02:00:00Z"),
            createdAt = now,
            description = "설명",
            ownerUserId = "user-1",
        )

    @Test
    fun `일정을 생성하면 저장된 일정을 반환한다`() =
        runTest {
            val saved = slot<Event>()
            coEvery { repo.save(capture(saved)) } answers { saved.captured }

            val result = service.createEvent(command)

            assertEquals("id-1", result.id)
            assertEquals("회의", result.title)
            assertEquals(now, result.createdAt)
            assertEquals("설명", result.description)
            assertEquals("user-1", result.ownerUserId)
            coVerify(exactly = 1) { repo.save(any()) }
        }

    @Test
    fun `제목이 공백이면 ValidationException이고 저장하지 않는다`() =
        runTest {
            assertFailsWith<ValidationException> {
                service.createEvent(command.copy(title = "   "))
            }

            coVerify(exactly = 0) { repo.save(any()) }
        }

    @Test
    fun `제목 길이가 200자를 초과하면 ValidationException이고 저장하지 않는다`() =
        runTest {
            assertFailsWith<ValidationException> {
                service.createEvent(command.copy(title = "a".repeat(201)))
            }

            coVerify(exactly = 0) { repo.save(any()) }
        }

    @Test
    fun `설명 길이가 2000자를 초과하면 ValidationException이고 저장하지 않는다`() =
        runTest {
            assertFailsWith<ValidationException> {
                service.createEvent(command.copy(description = "a".repeat(2001)))
            }

            coVerify(exactly = 0) { repo.save(any()) }
        }

    @Test
    fun `종료가 시작보다 빠르거나 같으면 ValidationException이고 저장하지 않는다`() =
        runTest {
            assertFailsWith<ValidationException> {
                service.createEvent(command.copy(end = command.start))
            }

            coVerify(exactly = 0) { repo.save(any()) }
        }

    @Test
    fun `특정 날짜의 일정을 조회하면 해당 날짜 구간과 겹치는 일정을 반환한다`() =
        runTest {
            coEvery { repo.findOverlapping(any(), any()) } returns listOf(event)

            val result = service.getEventsOn(LocalDate.parse("2026-10-26"), TimeZone.of("Asia/Seoul"))

            assertEquals(1, result.size)
            assertEquals("id-1", result[0].id)
            coVerify(exactly = 1) { repo.findOverlapping(any(), any()) }
        }

    @Test
    fun `존재하는 ID로 조회하면 일정을 반환한다`() =
        runTest {
            coEvery { repo.findById("id-1") } returns event

            val result = service.getEvent("id-1")

            assertEquals("id-1", result.id)
            coVerify(exactly = 1) { repo.findById("id-1") }
        }

    @Test
    fun `존재하지 않는 ID로 조회하면 NotFoundException을 던진다`() =
        runTest {
            coEvery { repo.findById("x") } returns null

            assertFailsWith<NotFoundException> {
                service.getEvent("x")
            }

            coVerify(exactly = 1) { repo.findById("x") }
        }

    @Test
    fun `존재하는 ID를 삭제하면 성공한다`() =
        runTest {
            coEvery { repo.deleteById("id-1") } returns true

            service.deleteEvent("id-1")

            coVerify(exactly = 1) { repo.deleteById("id-1") }
        }

    @Test
    fun `존재하지 않는 ID를 삭제하면 NotFoundException을 던진다`() =
        runTest {
            coEvery { repo.deleteById("x") } returns false

            assertFailsWith<NotFoundException> {
                service.deleteEvent("x")
            }

            coVerify(exactly = 1) { repo.deleteById("x") }
        }
}
