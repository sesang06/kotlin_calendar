package com.kotlincalendar.server.event.repository

import com.kotlincalendar.server.common.database.H2DatabaseProvider
import com.kotlincalendar.server.event.domain.Event
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExposedEventRepositoryTest {
    private val provider = H2DatabaseProvider("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1")
    private val database = provider.connect().also { transaction(it) { SchemaUtils.create(EventTable) } }
    private val repo = ExposedEventRepository(database)

    private fun event(
        id: String = UUID.randomUUID().toString(),
        title: String = "회의",
        start: Instant = Instant.parse("2026-10-26T01:00:00Z"),
        end: Instant = Instant.parse("2026-10-26T02:00:00Z"),
        createdAt: Instant = Instant.parse("2026-10-25T00:00:00Z"),
        description: String = "설명",
        ownerUserId: String = "user-1",
    ) = Event(
        id = id,
        title = title,
        start = start,
        end = end,
        createdAt = createdAt,
        description = description,
        ownerUserId = ownerUserId,
    )

    @Test
    fun `일정을 저장하고 ID로 조회할 수 있다`() =
        runTest {
            val created = repo.save(event(id = "id-1", title = "기획 회의"))

            val found = repo.findById("id-1")

            assertEquals(created, found)
        }

    @Test
    fun `존재하지 않는 ID 조회 시 null을 반환한다`() =
        runTest {
            val found = repo.findById("non-existent")

            assertNull(found)
        }

    @Test
    fun `겹치는 구간이 있으면 true, 경계가 맞닿기만 하면 false`() =
        runTest {
            repo.save(event(start = Instant.parse("2026-10-26T10:00:00Z"), end = Instant.parse("2026-10-26T11:00:00Z")))

            assertTrue(repo.existsOverlapping(Instant.parse("2026-10-26T10:30:00Z"), Instant.parse("2026-10-26T11:30:00Z")))
            assertFalse(repo.existsOverlapping(Instant.parse("2026-10-26T11:00:00Z"), Instant.parse("2026-10-26T12:00:00Z")))
            assertFalse(repo.existsOverlapping(Instant.parse("2026-10-26T09:00:00Z"), Instant.parse("2026-10-26T10:00:00Z")))
        }

    @Test
    fun `날짜로 조회 시 해당 일에 걸치는 일정들을 시작시각 오름차순으로 반환한다`() =
        runTest {
            val e2 =
                repo.save(
                    event(
                        id = "id-2",
                        start = Instant.parse("2026-10-26T05:00:00Z"),
                        end = Instant.parse("2026-10-26T06:00:00Z"),
                    ),
                )
            val e1 =
                repo.save(
                    event(
                        id = "id-1",
                        start = Instant.parse("2026-10-26T02:00:00Z"),
                        end = Instant.parse("2026-10-26T03:00:00Z"),
                    ),
                )

            val events = repo.findByDate(LocalDate.parse("2026-10-26"))

            assertEquals(2, events.size)
            assertEquals(e1.id, events[0].id)
            assertEquals(e2.id, events[1].id)
        }

    @Test
    fun `존재하는 일정을 삭제하면 true를 반환하고 조회되지 않는다`() =
        runTest {
            repo.save(event(id = "id-del"))

            val deleted = repo.deleteById("id-del")
            val found = repo.findById("id-del")

            assertTrue(deleted)
            assertNull(found)
        }

    @Test
    fun `존재하지 않는 일정을 삭제하면 false를 반환한다`() =
        runTest {
            val deleted = repo.deleteById("non-existent")

            assertFalse(deleted)
        }
}
