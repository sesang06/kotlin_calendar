package com.kotlincalendar.client.data.remote.auth

import kotlinx.coroutines.CancellationException

/**
 * 인증이 필요한 RemoteDataSource가 access token을 얻고, 거부되었을 때 갱신하는 창구.
 * SignUp·Login·RefreshToken 호출은 이 세션을 쓰지 않는다.
 */
interface AuthSession {
    /** 현재 저장된 access token. 로그인 전이면 null. */
    suspend fun accessToken(): String?

    /**
     * [rejectedToken]이 서버에서 거부된 뒤 호출한다. 갱신된 access token을 돌려주고,
     * 갱신할 수 없으면 예외를 던진다.
     */
    suspend fun refreshAccessToken(rejectedToken: String?): String
}

/**
 * 현재 access token으로 [block]을 실행하고, 401 / UNAUTHENTICATED이면 토큰을 한 번 갱신한 뒤 한 번만 재시도한다.
 * 갱신에 실패하면 원래의 인증 오류를 던진다 (갱신 오류는 suppressed로 첨부).
 */
suspend fun <T> AuthSession.withAuthRetry(block: suspend (accessToken: String?) -> T): T {
    val token = accessToken()
    try {
        return block(token)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        if (!e.isUnauthenticated()) throw e
        val refreshed =
            try {
                refreshAccessToken(token)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (refreshError: Throwable) {
                e.addSuppressed(refreshError)
                throw e
            }
        return block(refreshed)
    }
}
