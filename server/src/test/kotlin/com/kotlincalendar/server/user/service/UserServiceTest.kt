package com.kotlincalendar.server.user.service

import com.kotlincalendar.server.common.auth.PasswordHasher
import com.kotlincalendar.server.common.auth.TokenProvider
import com.kotlincalendar.server.common.auth.TokenResult
import com.kotlincalendar.server.common.error.ConflictException
import com.kotlincalendar.server.common.error.UnauthenticatedException
import com.kotlincalendar.server.common.error.ValidationException
import com.kotlincalendar.server.user.domain.LoginCommand
import com.kotlincalendar.server.user.domain.LogoutCommand
import com.kotlincalendar.server.user.domain.RefreshTokenCommand
import com.kotlincalendar.server.user.domain.SignUpCommand
import com.kotlincalendar.server.user.repository.RefreshTokenRecord
import com.kotlincalendar.server.user.repository.RefreshTokenRepository
import com.kotlincalendar.server.user.repository.UserRecord
import com.kotlincalendar.server.user.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class UserServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val tokenProvider = mockk<TokenProvider>()
    private val passwordHasher = mockk<PasswordHasher>()

    private val fixedNow = Instant.parse("2026-10-25T12:00:00Z")
    private val clock =
        object : Clock {
            override fun now(): Instant = fixedNow
        }

    private val service =
        UserService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            tokenProvider = tokenProvider,
            passwordHasher = passwordHasher,
            clock = clock,
            idGenerator = { "user-123" },
        )

    @Test
    fun `회원가입 성공 시 사용자 및 토큰을 반환하고 리포지토리에 저장한다`() =
        runTest {
            val command =
                SignUpCommand(
                    email = "User@Example.com",
                    password = "password123",
                    displayName = "홍길동",
                )

            coEvery { userRepository.existsByEmail("user@example.com") } returns false
            every { passwordHasher.hash("password123") } returns "hashed_pw"

            val savedUser = slot<UserRecord>()
            coEvery { userRepository.save(capture(savedUser)) } answers { savedUser.captured }

            every { tokenProvider.generateAccessToken("user-123", fixedNow) } returns
                TokenResult("access-token-1", fixedNow + 15.minutes)
            every { tokenProvider.generateRefreshToken("user-123", fixedNow) } returns
                TokenResult("refresh-token-1", fixedNow + 30.days)

            val savedRefreshToken = slot<RefreshTokenRecord>()
            coEvery { refreshTokenRepository.save(capture(savedRefreshToken)) } answers { savedRefreshToken.captured }

            val result = service.signUp(command)

            assertEquals("user-123", result.user.id)
            assertEquals("user@example.com", result.user.email)
            assertEquals("홍길동", result.user.displayName)
            assertEquals(fixedNow, result.user.createdAt)
            assertEquals("access-token-1", result.tokens.accessToken)
            assertEquals("refresh-token-1", result.tokens.refreshToken)

            assertEquals("user@example.com", savedUser.captured.email)
            assertEquals("hashed_pw", savedUser.captured.passwordHash)
            assertEquals("refresh-token-1", savedRefreshToken.captured.token)
        }

    @Test
    fun `이미 존재하는 이메일로 회원가입 시 ConflictException이 발생하고 저장하지 않는다`() =
        runTest {
            val command =
                SignUpCommand(
                    email = "test@example.com",
                    password = "password123",
                    displayName = "테스트",
                )

            coEvery { userRepository.existsByEmail("test@example.com") } returns true

            assertFailsWith<ConflictException> {
                service.signUp(command)
            }

            coVerify(exactly = 0) { userRepository.save(any()) }
            coVerify(exactly = 0) { refreshTokenRepository.save(any()) }
        }

    @Test
    fun `유효하지 않은 이메일 형식일 경우 ValidationException이 발생한다`() =
        runTest {
            val command =
                SignUpCommand(
                    email = "invalid-email",
                    password = "password123",
                    displayName = "테스트",
                )

            assertFailsWith<ValidationException> {
                service.signUp(command)
            }

            coVerify(exactly = 0) { userRepository.save(any()) }
        }

    @Test
    fun `비밀번호가 8자 미만이면 ValidationException이 발생한다`() =
        runTest {
            val command =
                SignUpCommand(
                    email = "test@example.com",
                    password = "short",
                    displayName = "테스트",
                )

            assertFailsWith<ValidationException> {
                service.signUp(command)
            }

            coVerify(exactly = 0) { userRepository.save(any()) }
        }

    @Test
    fun `로그인 성공 시 사용자와 새 토큰을 반환한다`() =
        runTest {
            val command = LoginCommand(email = "User@Example.com", password = "password123")
            val userRecord =
                UserRecord(
                    id = "user-123",
                    email = "user@example.com",
                    displayName = "홍길동",
                    passwordHash = "hashed_pw",
                    createdAtEpochMillis = fixedNow.toEpochMilliseconds(),
                )

            coEvery { userRepository.findByEmail("user@example.com") } returns userRecord
            every { passwordHasher.verify("password123", "hashed_pw") } returns true

            every { tokenProvider.generateAccessToken("user-123", fixedNow) } returns
                TokenResult("new-access-token", fixedNow + 15.minutes)
            every { tokenProvider.generateRefreshToken("user-123", fixedNow) } returns
                TokenResult("new-refresh-token", fixedNow + 30.days)

            val savedRefreshToken = slot<RefreshTokenRecord>()
            coEvery { refreshTokenRepository.save(capture(savedRefreshToken)) } answers { savedRefreshToken.captured }

            val result = service.login(command)

            assertEquals("user-123", result.user.id)
            assertEquals("new-access-token", result.tokens.accessToken)
            assertEquals("new-refresh-token", result.tokens.refreshToken)
        }

    @Test
    fun `비밀번호가 일치하지 않으면 UnauthenticatedException이 발생한다`() =
        runTest {
            val command = LoginCommand(email = "user@example.com", password = "wrong_password")
            val userRecord =
                UserRecord(
                    id = "user-123",
                    email = "user@example.com",
                    displayName = "홍길동",
                    passwordHash = "hashed_pw",
                    createdAtEpochMillis = fixedNow.toEpochMilliseconds(),
                )

            coEvery { userRepository.findByEmail("user@example.com") } returns userRecord
            every { passwordHasher.verify("wrong_password", "hashed_pw") } returns false

            assertFailsWith<UnauthenticatedException> {
                service.login(command)
            }

            coVerify(exactly = 0) { refreshTokenRepository.save(any()) }
        }

    @Test
    fun `존재하지 않는 사용자 이메일로 로그인 시 UnauthenticatedException이 발생한다`() =
        runTest {
            val command = LoginCommand(email = "nobody@example.com", password = "password123")
            coEvery { userRepository.findByEmail("nobody@example.com") } returns null

            assertFailsWith<UnauthenticatedException> {
                service.login(command)
            }
        }

    @Test
    fun `리프레시 토큰으로 갱신 시 기존 토큰을 폐기하고 새 토큰 쌍을 반환한다`() =
        runTest {
            val command = RefreshTokenCommand(refreshToken = "old-refresh-token")
            val tokenRecord =
                RefreshTokenRecord(
                    token = "old-refresh-token",
                    userId = "user-123",
                    expiresAt = fixedNow.toEpochMilliseconds() + 100_000,
                    createdAt = fixedNow.toEpochMilliseconds() - 100_000,
                    revoked = false,
                )
            val userRecord =
                UserRecord(
                    id = "user-123",
                    email = "user@example.com",
                    displayName = "홍길동",
                    passwordHash = "hashed_pw",
                    createdAtEpochMillis = fixedNow.toEpochMilliseconds() - 200_000,
                )

            coEvery { refreshTokenRepository.findByToken("old-refresh-token") } returns tokenRecord
            coEvery { refreshTokenRepository.revoke("old-refresh-token") } returns true
            coEvery { userRepository.findById("user-123") } returns userRecord

            every { tokenProvider.generateAccessToken("user-123", fixedNow) } returns
                TokenResult("new-access-token", fixedNow + 15.minutes)
            every { tokenProvider.generateRefreshToken("user-123", fixedNow) } returns
                TokenResult("new-refresh-token", fixedNow + 30.days)

            val savedRefreshToken = slot<RefreshTokenRecord>()
            coEvery { refreshTokenRepository.save(capture(savedRefreshToken)) } answers { savedRefreshToken.captured }

            val result = service.refreshToken(command)

            assertEquals("new-access-token", result.accessToken)
            assertEquals("new-refresh-token", result.refreshToken)
            coVerify(exactly = 1) { refreshTokenRepository.revoke("old-refresh-token") }
        }

    @Test
    fun `만료된 리프레시 토큰인 경우 UnauthenticatedException이 발생한다`() =
        runTest {
            val command = RefreshTokenCommand(refreshToken = "expired-token")
            val tokenRecord =
                RefreshTokenRecord(
                    token = "expired-token",
                    userId = "user-123",
                    expiresAt = fixedNow.toEpochMilliseconds() - 1,
                    createdAt = fixedNow.toEpochMilliseconds() - 100_000,
                    revoked = false,
                )

            coEvery { refreshTokenRepository.findByToken("expired-token") } returns tokenRecord

            assertFailsWith<UnauthenticatedException> {
                service.refreshToken(command)
            }

            coVerify(exactly = 0) { refreshTokenRepository.revoke(any()) }
        }

    @Test
    fun `로그아웃 시 자신의 리프레시 토큰이면 폐기한다`() =
        runTest {
            val command = LogoutCommand(refreshToken = "my-refresh-token", userId = "user-123")
            val tokenRecord =
                RefreshTokenRecord(
                    token = "my-refresh-token",
                    userId = "user-123",
                    expiresAt = fixedNow.toEpochMilliseconds() + 100_000,
                    createdAt = fixedNow.toEpochMilliseconds(),
                    revoked = false,
                )

            coEvery { refreshTokenRepository.findByToken("my-refresh-token") } returns tokenRecord
            coEvery { refreshTokenRepository.revoke("my-refresh-token") } returns true

            service.logout(command)

            coVerify(exactly = 1) { refreshTokenRepository.revoke("my-refresh-token") }
        }

    @Test
    fun `로그아웃 시 다른 사용자의 리프레시 토큰이면 폐기하지 않는다`() =
        runTest {
            val command = LogoutCommand(refreshToken = "other-refresh-token", userId = "user-123")
            val tokenRecord =
                RefreshTokenRecord(
                    token = "other-refresh-token",
                    userId = "other-user",
                    expiresAt = fixedNow.toEpochMilliseconds() + 100_000,
                    createdAt = fixedNow.toEpochMilliseconds(),
                    revoked = false,
                )

            coEvery { refreshTokenRepository.findByToken("other-refresh-token") } returns tokenRecord

            service.logout(command)

            coVerify(exactly = 0) { refreshTokenRepository.revoke(any()) }
        }

    @Test
    fun `getMe는 사용자 정보를 반환한다`() =
        runTest {
            coEvery { userRepository.findById("user-123") } returns
                UserRecord(
                    id = "user-123",
                    email = "user@example.com",
                    displayName = "홍길동",
                    passwordHash = "hash",
                    createdAtEpochMillis = fixedNow.toEpochMilliseconds(),
                )

            val user = service.getMe("user-123")

            assertEquals("user@example.com", user.email)
            assertEquals("홍길동", user.displayName)
            assertEquals(fixedNow, user.createdAt)
        }

    @Test
    fun `getMe에서 사용자가 없으면 UnauthenticatedException`() =
        runTest {
            coEvery { userRepository.findById("gone") } returns null

            assertFailsWith<UnauthenticatedException> { service.getMe("gone") }
        }
}
