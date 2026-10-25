package com.kotlincalendar.client.android

import android.app.Application
import com.kotlincalendar.client.di.initKoinAndroid

class KotlinCalendarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
