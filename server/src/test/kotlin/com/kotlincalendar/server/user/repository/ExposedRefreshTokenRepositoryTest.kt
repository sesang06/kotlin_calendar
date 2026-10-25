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

class ExposedRefreshTokenRepositoryTest {
    private val provider = H2DatabaseProvider("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1")
    private val database =
        provider.connect().also { db ->
            transaction(db) { SchemaUtils.create(RefreshTokenTable) }
        }
    private val repository = ExposedRefreshTokenRepository(database)

    @Test
    fun `리프레시 토큰을 저장하고 토큰값으로 조회할 수 있다`() =
        runTest {
            val record =
                RefreshTokenRecord(
                    token = "token-abc",
                    userId = "user-1",
                    expiresAt = 2000L,
                    createdAt = 1000L,
                    revoked = false,
                )

            repository.save(record)

            val found = repository.findByToken("token-abc")
            assertNotNull(found)
            assertEquals("user-1", found.userId)
            assertEquals(2000L, found.expiresAt)
            assertFalse(found.revoked)

            assertNull(repository.findByToken("non-existent"))
        }

    @Test
    fun `revoke 호출 시 해당 토큰이 폐기 상태로 변경된다`() =
        runTest {
            val record =
                RefreshTokenRecord(
                    token = "token-to-revoke",
                    userId = "user-1",
                    expiresAt = 2000L,
                    createdAt = 1000L,
                    revoked = false,
                )

            repository.save(record)

            val revoked = repository.revoke("token-to-revoke")
            assertTrue(revoked)

            val found = repository.findByToken("token-to-revoke")
            assertNotNull(found)
            assertTrue(found.revoked)
        }

    @Test
    fun `revokeAllByUserId 호출 시 해당 사용자의 모든 토큰이 폐기된다`() =
        runTest {
            repository.save(
                RefreshTokenRecord(
                    token = "token-1",
                    userId = "user-multi",
                    expiresAt = 2000L,
                    createdAt = 1000L,
                    revoked = false,
                ),
            )
            repository.save(
                RefreshTokenRecord(
                    token = "token-2",
                    userId = "user-multi",
                    expiresAt = 2000L,
                    createdAt = 1000L,
                    revoked = false,
                ),
            )

            val count = repository.revokeAllByUserId("user-multi")
            assertEquals(2, count)

            assertTrue(repository.findByToken("token-1")!!.revoked)
            assertTrue(repository.findByToken("token-2")!!.revoked)
        }
}
