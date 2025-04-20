package com.example.autapp

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class AUTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}