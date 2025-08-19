package com.example.automaticfinances

import android.app.Application
import com.example.automaticfinances.data.db.AppDatabase

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.init(this)
    }
}