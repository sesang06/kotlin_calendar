package com.kotlincalendar.client.data.remote.auth

import com.kotlincalendar.client.domain.repository.AuthRepository
import com.kotlincalendar.client.domain.repository.TokenStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [TokenStorage]에서 access token을 읽고, 거부되면 [AuthRepository.refreshToken]으로 갱신한다.
 *
 * 동시에 여러 요청이 401을 받아도 refresh는 한 번만 일어나도록 [Mutex]로 직렬화한다.
 * 다른 요청이 이미 토큰을 바꿔 두었으면 refresh 없이 그 토큰을 돌려준다.
 */
class TokenAuthSession(
    private val tokenStorage: TokenStorage,
    private val authRepository: AuthRepository,
) : AuthSession {
    private val refreshMutex = Mutex()

    override suspend fun accessToken(): String? = tokenStorage.getTokens()?.accessToken

    override suspend fun refreshAccessToken(rejectedToken: String?): String =
        refreshMutex.withLock {
            val current = tokenStorage.getTokens()?.accessToken
            if (current != null && current != rejectedToken) {
                current
            } else {
                authRepository.refreshToken().getOrThrow().accessToken
            }
        }
}
