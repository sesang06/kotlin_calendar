package com.kotlincalendar.server.common.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class TokenProviderTest {
    private val tokenProvider = HmacTokenProvider()
    private val now = Instant.parse("2026-10-25T12:00:00Z")

    @Test
    fun `액세스 토큰 생성 및 검증이 올바르게 동작한다`() {
        val result = tokenProvider.generateAccessToken("user-abc", now)

        val verifiedUserId = tokenProvider.verifyAccessToken(result.token, now)
        assertEquals("user-abc", verifiedUserId)
    }

    @Test
    fun `만료된 액세스 토큰 검증 시 null을 반환한다`() {
        val result = tokenProvider.generateAccessToken("user-abc", now)

        val future = now + 2.hours
        val verifiedUserId = tokenProvider.verifyAccessToken(result.token, future)
        assertNull(verifiedUserId)
    }

    @Test
    fun `변조된 토큰 검증 시 null을 반환한다`() {
        val result = tokenProvider.generateAccessToken("user-abc", now)
        val tampered = result.token + "tampered"

        assertNull(tokenProvider.verifyAccessToken(tampered, now))
    }

    @Test
    fun `리프레시 토큰 생성이 올바르게 동작한다`() {
        val result = tokenProvider.generateRefreshToken("user-abc", now)

        assertNotNull(result.token)
        assertTrue(result.expiresAt > now)
    }
}
