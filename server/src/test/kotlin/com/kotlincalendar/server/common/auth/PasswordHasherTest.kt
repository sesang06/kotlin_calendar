package com.kotlincalendar.server.common.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordHasherTest {
    private val hasher = Pbkdf2PasswordHasher()

    @Test
    fun `비밀번호 해시 생성 및 검증이 올바르게 동작한다`() {
        val password = "mySecurePassword123"
        val hash = hasher.hash(password)

        assertTrue(hasher.verify(password, hash))
        assertFalse(hasher.verify("wrongPassword", hash))
    }

    @Test
    fun `잘못된 형식의 해시는 false를 반환한다`() {
        assertFalse(hasher.verify("pass", "invalid_hash_string"))
        assertFalse(hasher.verify("pass", "1000:invalid:base64!"))
    }
}
