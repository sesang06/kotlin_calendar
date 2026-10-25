package com.kotlincalendar.server.common.database

import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/** `TIMESTAMPTZ` 컬럼(Exposed `timestampWithTimeZone`)과 도메인 [Instant] 변환. 저장은 UTC. */
fun Instant.toUtcOffsetDateTime(): OffsetDateTime = OffsetDateTime.ofInstant(toJavaInstant(), ZoneOffset.UTC)

fun OffsetDateTime.toKotlinInstant(): Instant = toInstant().toKotlinInstant()

fun Long.epochMillisToUtcOffsetDateTime(): OffsetDateTime = Instant.fromEpochMilliseconds(this).toUtcOffsetDateTime()

fun OffsetDateTime.toEpochMillis(): Long = toInstant().toEpochMilli()
