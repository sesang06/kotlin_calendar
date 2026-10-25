package com.kotlincalendar.client.data.mapper

import com.kotlincalendar.client.domain.model.AuthTokens
import com.kotlincalendar.client.domain.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import com.kotlincalendar.proto.auth.v1.AuthTokens as ProtoAuthTokens
import com.kotlincalendar.proto.auth.v1.User as ProtoUser

class AuthProtoMapperTest {
    @Test
    fun userToDomainAndToProtoMapping() {
        val instant = Instant.fromEpochMilliseconds(1_700_000_000_000L)
        val proto =
            ProtoUser(
                id = "user-123",
                email = "user@example.com",
                display_name = "User Name",
                created_at_epoch_millis = 1_700_000_000_000L,
            )

        val domain = proto.toDomain()
        assertEquals(
            User(
                id = "user-123",
                email = "user@example.com",
                displayName = "User Name",
                createdAt = instant,
            ),
            domain,
        )

        val backToProto = domain.toProto()
        assertEquals(proto, backToProto)
    }

    @Test
    fun authTokensToDomainAndToProtoMapping() {
        val accessExp = Instant.fromEpochMilliseconds(1_700_000_900_000L)
        val refreshExp = Instant.fromEpochMilliseconds(1_700_086_400_000L)

        val proto =
            ProtoAuthTokens(
                access_token = "access-token-xyz",
                access_token_expires_at_epoch_millis = 1_700_000_900_000L,
                refresh_token = "refresh-token-abc",
                refresh_token_expires_at_epoch_millis = 1_700_086_400_000L,
            )

        val domain = proto.toDomain()
        assertEquals(
            AuthTokens(
                accessToken = "access-token-xyz",
                accessTokenExpiresAt = accessExp,
                refreshToken = "refresh-token-abc",
                refreshTokenExpiresAt = refreshExp,
            ),
            domain,
        )

        val backToProto = domain.toProto()
        assertEquals(proto, backToProto)
    }
}
