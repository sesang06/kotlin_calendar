package com.kotlincalendar.server.user.repository

import com.kotlincalendar.server.common.database.migratedH2Database
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExposedRefreshTokenRepositoryTest {
    private val database = migratedH2Database()
    private val repository = ExposedRefreshTokenRepository(database)

    @BeforeTest
    fun setUpUsers() =
        runTest {
            val users = ExposedUserRepository(database)
            listOf("user-1", "user-multi").forEach { id ->
                users.save(
                    UserRecord(
                        id = id,
                        email = "$id@example.com",
                        displayName = id,
                        passwordHash = "hash",
                        createdAtEpochMillis = 0L,
                    ),
                )
            }
        }

    private fun record(
        id: String,
        token: String,
        userId: String = "user-1",
    ) = RefreshTokenRecord(
        id = id,
        token = token,
        userId = userId,
        expiresAt = 2000L,
        createdAt = 1000L,
    )

    @Test
    fun `리프레시 토큰을 저장하고 토큰값으로 조회할 수 있다`() =
        runTest {
            repository.save(record(id = "t1", token = "token-abc"))

            val found = repository.findByToken("token-abc")
            assertNotNull(found)
            assertEquals("t1", found.id)
            assertEquals("token-abc", found.token)
            assertEquals("user-1", found.userId)
            assertEquals(2000L, found.expiresAt)
            assertFalse(found.revoked)

            assertNull(repository.findByToken("non-existent"))
        }

    @Test
    fun `원문 토큰은 저장하지 않고 SHA-256 해시만 저장한다`() =
        runTest {
            repository.save(record(id = "t1", token = "token-abc"))

            val stored = transaction(database) { RefreshTokenTable.selectAll().single()[RefreshTokenTable.tokenHash] }

            assertEquals(64, stored.length)
            assertNotEquals("token-abc", stored)
        }

    @Test
    fun `revoke 호출 시 해당 토큰이 폐기 상태로 변경된다`() =
        runTest {
            repository.save(record(id = "t1", token = "token-to-revoke"))

            assertTrue(repository.revoke("token-to-revoke", revokedAt = 1500L))

            val found = repository.findByToken("token-to-revoke")
            assertNotNull(found)
            assertTrue(found.revoked)
            assertEquals(1500L, found.revokedAt)
        }

    @Test
    fun `이미 폐기된 토큰을 다시 revoke하면 false를 반환하고 폐기 시각은 유지된다`() =
        runTest {
            repository.save(record(id = "t1", token = "token-to-revoke"))
            repository.revoke("token-to-revoke", revokedAt = 1500L)

            assertFalse(repository.revoke("token-to-revoke", revokedAt = 1800L))
            assertEquals(1500L, repository.findByToken("token-to-revoke")?.revokedAt)
        }

    @Test
    fun `revokeAllByUserId 호출 시 해당 사용자의 모든 토큰이 폐기된다`() =
        runTest {
            repository.save(record(id = "t1", token = "token-1", userId = "user-multi"))
            repository.save(record(id = "t2", token = "token-2", userId = "user-multi"))
            repository.save(record(id = "t3", token = "token-3", userId = "user-1"))

            val count = repository.revokeAllByUserId("user-multi", revokedAt = 1500L)
            assertEquals(2, count)

            assertTrue(repository.findByToken("token-1")!!.revoked)
            assertTrue(repository.findByToken("token-2")!!.revoked)
            assertFalse(repository.findByToken("token-3")!!.revoked)
        }
}
