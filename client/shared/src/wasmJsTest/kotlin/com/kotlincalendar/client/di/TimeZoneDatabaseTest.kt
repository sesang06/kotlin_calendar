package com.kotlincalendar.client.di

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class TimeZoneDatabaseTest {
    @Test
    fun platformModule_loadsIanaTimeZoneDatabase() {
        // PlatformModule.wasmJs.kt 가 @js-joda/timezone 을 로드한다.
        assertNotNull(platformModule)

        assertEquals("Asia/Seoul", TimeZone.of("Asia/Seoul").id)
        // 시간대 DB가 없으면 "SYSTEM"이 되어 서버가 400(invalid time_zone)으로 거절한다.
        assertNotEquals("SYSTEM", TimeZone.currentSystemDefault().id)
    }
}
