package com.kotlincalendar.client.di

import android.content.Context
import org.koin.android.ext.koin.androidContext

/** Android 진입점(Application)에서 한 번 호출한다. */
fun initKoinAndroid(context: Context) =
    initKoin {
        androidContext(context.applicationContext)
    }
