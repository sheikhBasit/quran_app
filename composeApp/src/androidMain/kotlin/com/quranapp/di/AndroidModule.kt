package com.quranapp.di

import com.quranapp.util.DatabaseDriverFactory
import com.quranapp.util.SettingsStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { SettingsStore(androidContext()) }
    single { com.quranapp.util.LocationProvider(androidContext()) }
    single { com.quranapp.util.CompassSensor(androidContext()) }
    single { com.quranapp.util.NotificationScheduler(androidContext()) }
    single(org.koin.core.qualifier.named("baseUrl")) { com.quranapp.BuildConfig.BACKEND_URL }
}
