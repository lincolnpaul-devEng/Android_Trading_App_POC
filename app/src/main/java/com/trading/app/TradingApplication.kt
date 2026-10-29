package com.trading.app

import android.app.Application
import app.rive.runtime.kotlin.core.Rive
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TradingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            Rive.init(this)
        } catch (e: Exception) {
            // Rive already initialized or fallback
        }
    }

    companion object {
        @Volatile
        var instance: TradingApplication? = null
            private set
    }
}
