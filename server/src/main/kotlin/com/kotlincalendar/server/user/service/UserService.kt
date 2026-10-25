package com.kotlincalendar.server.user.service

import com.kotlincalendar.server.common.auth.PasswordHasher
import com.kotlincalendar.server.common.auth.Pbkdf2PasswordHasher
import com.kotlincalendar.server.common.auth.TokenProvider
import com.kotlincalendar.server.common.error.ConflictException
import com.kotlincalendar.server.common.error.UnauthenticatedException
import com.kotlincalendar.server.common.error.ValidationException
import com.kotlincalendar.server.user.domain.AuthResult
import com.kotlincalendar.server.user.domain.AuthTokens
import com.kotlincalendar.server.user.domain.LoginCommand
import com.kotlincalendar.server.user.domain.LogoutCommand
import com.kotlincalendar.server.user.domain.RefreshTokenCommand
import com.kotlincalendar.server.user.domain.SignUpCommand
import com.kotlincalendar.server.user.domain.User
import com.kotlincalendar.server.user.repository.RefreshTokenRecord
import com.kotlincalendar.server.user.repository.RefreshTokenRepository
import com.kotlincalendar.server.user.repository.UserRecord
import com.kotlincalendar.server.user.repository.UserRepository
import java.util.UUID
import kotlin.time.Clock

class UserService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenProvider: TokenProvider,
    private val passwordHasher: PasswordHasher = Pbkdf2PasswordHasher(),
    private val clock: Clock = Clock.System,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    private val emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$".toRegex()

    suspend fun signUp(command: SignUpCommand): AuthResult {
        val normalizedEmail = normalizeEmail(command.email)
        validatePassword(command.password)
        validateDisplayName(command.displayName)

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw ConflictException("email '$normalizedEmail' is already registered")
        }

        val now = clock.now()
        val userId = idGenerator()
        val passwordHash = passwordHasher.hash(command.password)

        val userRecord =
            UserRecord(
                id = userId,
                email = normalizedEmail,
                displayName = command.displayName.trim(),
                passwordHash = passwordHash,
                createdAtEpochMillis = now.toEpochMilliseconds(),
            )
        userRepository.save(userRecord)

        val accessToken = tokenProvider.generateAccessToken(userId, now)
        val refreshToken = tokenProvider.generateRefreshToken(userId, now)

        refreshTokenRepository.save(
            RefreshTokenRecord(
                token = refreshToken.token,
                userId = userId,
                expiresAt = refreshToken.expiresAt.toEpochMilliseconds(),
                createdAt = now.toEpochMilliseconds(),
                revoked = false,
            ),
        )

        return AuthResult(
            user = userRecord.toDomain(),
            tokens =
                AuthTokens(
                    accessToken = accessToken.token,
                    accessTokenExpiresAt = accessToken.expiresAt,
                    refreshToken = refreshToken.token,
                    refreshTokenExpiresAt = refreshToken.expiresAt,
                ),
        )
    }

    suspend fun login(command: LoginCommand): AuthResult {
        if (command.email.isBlank() || command.password.isBlank()) {
            throw UnauthenticatedException("invalid email or password")
        }
        val normalizedEmail = command.email.trim().lowercase()
        val userRecord =
            userRepository.findByEmail(normalizedEmail)
                ?: throw UnauthenticatedException("invalid email or password")

        if (!passwordHasher.verify(command.password, userRecord.passwordHash)) {
            throw UnauthenticatedException("invalid email or password")
        }

        val now = clock.now()
        val accessToken = tokenProvider.generateAccessToken(userRecord.id, now)
        val refreshToken = tokenProvider.generateRefreshToken(userRecord.id, now)

        refreshTokenRepository.save(
            RefreshTokenRecord(
                token = refreshToken.token,
                userId = userRecord.id,
                expiresAt = refreshToken.expiresAt.toEpochMilliseconds(),
                createdAt = now.toEpochMilliseconds(),
                revoked = false,
            ),
        )

        return AuthResult(
            user = userRecord.toDomain(),
            tokens =
                AuthTokens(
                    accessToken = accessToken.token,
                    accessTokenExpiresAt = accessToken.expiresAt,
                    refreshToken = refreshToken.token,
                    refreshTokenExpiresAt = refreshToken.expiresAt,
                ),
        )
    }

    suspend fun refreshToken(command: RefreshTokenCommand): AuthTokens {
        if (command.refreshToken.isBlank()) {
            throw ValidationException("refresh token is required")
        }
        val tokenRecord =
            refreshTokenRepository.findByToken(command.refreshToken)
                ?: throw UnauthenticatedException("invalid or expired refresh token")

        val now = clock.now()
        if (tokenRecord.revoked || tokenRecord.expiresAt <= now.toEpochMilliseconds()) {
            throw UnauthenticatedException("invalid or expired refresh token")
        }

        refreshTokenRepository.revoke(command.refreshToken)

        val userRecord =
            userRepository.findById(tokenRecord.userId)
                ?: throw UnauthenticatedException("user not found")

        val accessToken = tokenProvider.generateAccessToken(userRecord.id, now)
        val newRefreshToken = tokenProvider.generateRefreshToken(userRecord.id, now)

        refreshTokenRepository.save(
            RefreshTokenRecord(
                token = newRefreshToken.token,
                userId = userRecord.id,
                expiresAt = newRefreshToken.expiresAt.toEpochMilliseconds(),
                createdAt = now.toEpochMilliseconds(),
                revoked = false,
            ),
        )

        return AuthTokens(
            accessToken = accessToken.token,
            accessTokenExpiresAt = accessToken.expiresAt,
            refreshToken = newRefreshToken.token,
            refreshTokenExpiresAt = newRefreshToken.expiresAt,
        )
    }

    suspend fun logout(command: LogoutCommand) {
        if (command.refreshToken.isBlank()) {
            throw ValidationException("refresh token is required")
        }
        val tokenRecord = refreshTokenRepository.findByToken(command.refreshToken)
        if (tokenRecord != null && tokenRecord.userId == command.userId) {
            refreshTokenRepository.revoke(command.refreshToken)
        }
    }

    /** access token의 사용자. 토큰은 유효하지만 사용자가 없으면(삭제 등) 인증 실패로 본다. */
    suspend fun getMe(userId: String): User =
        userRepository.findById(userId)?.toDomain()
            ?: throw UnauthenticatedException("user not found")

    private fun normalizeEmail(email: String): String {
        val trimmed = email.trim()
        if (trimmed.length > 320 || !emailRegex.matches(trimmed)) {
            throw ValidationException("invalid email format '$email'")
        }
        return trimmed.lowercase()
    }

    private fun validatePassword(password: String) {
        if (password.length !in 8..72) {
            throw ValidationException("password must be between 8 and 72 characters")
        }
    }

    private fun validateDisplayName(displayName: String) {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty() || trimmed.length > 100) {
            throw ValidationException("display name must be between 1 and 100 characters")
        }
    }
}
