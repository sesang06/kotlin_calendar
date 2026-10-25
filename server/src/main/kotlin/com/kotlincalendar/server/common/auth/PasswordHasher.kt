package com.kotlincalendar.server.common.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

interface PasswordHasher {
    fun hash(password: String): String

    fun verify(
        password: String,
        hash: String,
    ): Boolean
}

class Pbkdf2PasswordHasher(
    private val iterations: Int = 10_000,
    private val keyLength: Int = 256,
) : PasswordHasher {
    private val random = SecureRandom()

    override fun hash(password: String): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hashB64 = Base64.getEncoder().encodeToString(hash)
        return "$iterations:$saltB64:$hashB64"
    }

    override fun verify(
        password: String,
        hash: String,
    ): Boolean {
        val parts = hash.split(":")
        if (parts.size != 3) return false
        val iter = parts[0].toIntOrNull() ?: return false
        val salt = runCatching { Base64.getDecoder().decode(parts[1]) }.getOrNull() ?: return false
        val expectedHash = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false

        val spec = PBEKeySpec(password.toCharArray(), salt, iter, expectedHash.size * 8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val actualHash = factory.generateSecret(spec).encoded
        return MessageDigest.isEqual(expectedHash, actualHash)
    }
}
