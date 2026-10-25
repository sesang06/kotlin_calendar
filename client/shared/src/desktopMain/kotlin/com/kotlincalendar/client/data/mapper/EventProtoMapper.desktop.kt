package com.kotlincalendar.client.data.mapper

import kotlin.time.Instant

actual fun com.squareup.wire.Instant.toKotlinInstant(): Instant = Instant.fromEpochSeconds(epochSecond, nano.toLong())

actual fun Instant.toWireInstant(): com.squareup.wire.Instant =
    com.squareup.wire.Instant
        .ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())
