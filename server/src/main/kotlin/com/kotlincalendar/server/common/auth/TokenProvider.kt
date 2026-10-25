package com.kotlincalendar.server.common.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class TokenResult(
    val token: String,
    val expiresAt: Instant,
)

interface TokenProvider {
    fun generateAccessToken(
        userId: String,
        now: Instant,
    ): TokenResult

    fun generateRefreshToken(
        userId: String,
        now: Instant,
    ): TokenResult

    fun verifyAccessToken(
        token: String,
        now: Instant,
    ): String?
}

class HmacTokenProvider(
    secret: ByteArray = ByteArray(32).apply { SecureRandom().nextBytes(this) },
    private val accessTokenDuration: Duration = 15.minutes,
    private val refreshTokenDuration: Duration = 30.days,
) : TokenProvider {
    private val secretKey = SecretKeySpec(secret, "HmacSHA256")
    private val random = SecureRandom()

    override fun generateAccessToken(
        userId: String,
        now: Instant,
    ): TokenResult {
        val expiresAt = now + accessTokenDuration
        val expiresAtMillis = expiresAt.toEpochMilliseconds()
        val payload = "$userId:$expiresAtMillis"
        val payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray(Charsets.UTF_8))

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val signature = mac.doFinal(payloadB64.toByteArray(Charsets.UTF_8))
        val sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signature)

        val token = "$payloadB64.$sigB64"
        return TokenResult(token, expiresAt)
    }

    override fun generateRefreshToken(
        userId: String,
        now: Instant,
    ): TokenResult {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val expiresAt = now + refreshTokenDuration
        return TokenResult(token, expiresAt)
    }

    override fun verifyAccessToken(
        token: String,
        now: Instant,
    ): String? {
        val parts = token.split(".")
        if (parts.size != 2) return null
        val payloadB64 = parts[0]
        val sigB64 = parts[1]

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val expectedSig = mac.doFinal(payloadB64.toByteArray(Charsets.UTF_8))
        val actualSig = runCatching { Base64.getUrlDecoder().decode(sigB64) }.getOrNull() ?: return null

        if (!MessageDigest.isEqual(expectedSig, actualSig)) return null

        val payload =
            runCatching { String(Base64.getUrlDecoder().decode(payloadB64), Charsets.UTF_8) }.getOrNull()
                ?: return null

        val payloadParts = payload.split(":")
        if (payloadParts.size != 2) return null
        val userId = payloadParts[0]
        val expiresAtMillis = payloadParts[1].toLongOrNull() ?: return null

        if (now.toEpochMilliseconds() > expiresAtMillis) return null

        return userId
    }
}
