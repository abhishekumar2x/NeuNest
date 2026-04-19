package com.error404.neunest

import android.app.Application
import com.error404.neunest.di.appModule
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)

        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
    }
}