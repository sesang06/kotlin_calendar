package com.kotlincalendar.client.fake

import com.kotlincalendar.client.data.remote.auth.AuthSession

class FakeAuthSession(
    var token: String? = "access-1",
) : AuthSession {
    /** refresh 결과. 성공이면 [token]도 그 값으로 바뀐다. */
    var refreshResult: Result<String> = Result.success("access-2")
    val rejectedTokens = mutableListOf<String?>()

    override suspend fun accessToken(): String? = token

    override suspend fun refreshAccessToken(rejectedToken: String?): String {
        rejectedTokens += rejectedToken
        return refreshResult.getOrThrow().also { token = it }
    }
}
