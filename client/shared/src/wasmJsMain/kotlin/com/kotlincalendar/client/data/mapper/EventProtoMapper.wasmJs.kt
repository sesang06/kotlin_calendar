package com.kotlincalendar.client.data.mapper

import kotlin.time.Instant

actual fun com.squareup.wire.Instant.toKotlinInstant(): Instant = Instant.fromEpochSeconds(getEpochSecond(), getNano().toLong())

actual fun Instant.toWireInstant(): com.squareup.wire.Instant = com.squareup.wire.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())
