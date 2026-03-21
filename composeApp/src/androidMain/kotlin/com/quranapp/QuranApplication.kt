package com.quranapp

import android.app.Application
import com.quranapp.di.androidModule
import com.quranapp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class QuranApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QuranApplication)
            modules(appModule, androidModule)
        }
    }
}
