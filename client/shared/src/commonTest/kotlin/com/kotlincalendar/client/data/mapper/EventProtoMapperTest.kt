package com.kotlincalendar.client.data.mapper

import com.squareup.wire.ofEpochSecond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import com.kotlincalendar.proto.calendar.v1.Event as ProtoEvent

class EventProtoMapperTest {
    @Test
    fun protoEventToDomain_mapsAllFieldsCorrectly() {
        val startInstant = Instant.fromEpochSeconds(1700000000, 500000000)
        val endInstant = Instant.fromEpochSeconds(1700003600, 0)
        val createdInstant = Instant.fromEpochSeconds(1699990000, 0)

        val proto =
            ProtoEvent(
                id = "evt-123",
                title = "Team Meeting",
                start_time = startInstant.toWireInstant(),
                end_time = endInstant.toWireInstant(),
                created_at = createdInstant.toWireInstant(),
                description = "Weekly sync",
                owner_user_id = "usr-456",
            )

        val domain = proto.toDomain()

        assertEquals("evt-123", domain.id)
        assertEquals("Team Meeting", domain.title)
        assertEquals(startInstant, domain.start)
        assertEquals(endInstant, domain.end)
        assertEquals(createdInstant, domain.createdAt)
        assertEquals("Weekly sync", domain.description)
        assertEquals("usr-456", domain.ownerUserId)
    }

    @Test
    fun protoEventToDomain_handlesNullTimestampsWithFallback() {
        val proto =
            ProtoEvent(
                id = "evt-empty",
                title = "No Times",
                start_time = null,
                end_time = null,
                created_at = null,
                description = "",
                owner_user_id = "",
            )

        val domain = proto.toDomain()

        assertEquals(Instant.DISTANT_PAST, domain.start)
        assertEquals(Instant.DISTANT_PAST, domain.end)
        assertNull(domain.createdAt)
    }

    @Test
    fun toCreateEventRequest_encodesFieldsProperly() {
        val start = Instant.fromEpochSeconds(1700000000, 0)
        val end = Instant.fromEpochSeconds(1700003600, 0)

        val request =
            toCreateEventRequest(
                title = "New Event",
                startTime = start,
                endTime = end,
                description = "Description",
            )

        assertEquals("New Event", request.title)
        assertEquals("Description", request.description)
        assertEquals(start.toWireInstant(), request.start_time)
        assertEquals(end.toWireInstant(), request.end_time)
    }

    @Test
    fun toWireInstant_setsEpochSecondAndNano() {
        val wire = Instant.fromEpochSeconds(1700000000, 500000000).toWireInstant()

        assertEquals(1700000000L, wire.getEpochSecond())
        assertEquals(500000000, wire.getNano())
    }

    @Test
    fun wireInstantConversion_handlesPreEpochWithNanos() {
        val wire = ofEpochSecond(-1, 250000000)

        val instant = wire.toKotlinInstant()

        assertEquals(-750L, instant.toEpochMilliseconds())
        assertEquals(-1L, instant.toWireInstant().getEpochSecond())
        assertEquals(250000000, instant.toWireInstant().getNano())
    }
}
