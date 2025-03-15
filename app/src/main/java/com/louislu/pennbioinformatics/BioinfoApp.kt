package com.louislu.pennbioinformatics

import android.app.Application
import timber.log.Timber

class BioinfoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree()) // Enable Timber logs
    }
}