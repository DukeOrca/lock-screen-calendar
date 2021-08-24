package com.duke.orca.android.kotlin.lockscreencalendar.main

import android.app.Application
import com.duke.orca.android.kotlin.lockscreencalendar.BuildConfig
import timber.log.Timber

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    companion object {
        lateinit var INSTANCE: MainApplication
    }
}