package com.kotlincalendar.server.user.repository

import com.kotlincalendar.server.common.database.H2DatabaseProvider
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExposedUserRepositoryTest {
    private val provider = H2DatabaseProvider("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1")
    private val database =
        provider.connect().also { db ->
            transaction(db) { SchemaUtils.create(UserTable) }
        }
    private val repository = ExposedUserRepository(database)

    @Test
    fun `사용자를 저장하고 ID 및 이메일로 조회할 수 있다`() =
        runTest {
            val user =
                UserRecord(
                    id = "user-1",
                    email = "test@example.com",
                    displayName = "Tester",
                    passwordHash = "hash123",
                    createdAtEpochMillis = 1000L,
                )

            repository.save(user)

            val byId = repository.findById("user-1")
            assertNotNull(byId)
            assertEquals("test@example.com", byId.email)
            assertEquals("Tester", byId.displayName)
            assertEquals("hash123", byId.passwordHash)

            val byEmail = repository.findByEmail("test@example.com")
            assertNotNull(byEmail)
            assertEquals("user-1", byEmail.id)

            val notFound = repository.findById("none")
            assertNull(notFound)
        }

    @Test
    fun `existsByEmail은 이메일 존재 여부를 정확히 반환한다`() =
        runTest {
            val user =
                UserRecord(
                    id = "user-2",
                    email = "exists@example.com",
                    displayName = "Tester 2",
                    passwordHash = "hash",
                    createdAtEpochMillis = 1000L,
                )

            repository.save(user)

            assertTrue(repository.existsByEmail("exists@example.com"))
            assertFalse(repository.existsByEmail("not_exists@example.com"))
        }
}
