package com.hinvr.app

import android.app.Application
import com.hinvr.app.data.SessionRepository
import com.hinvr.app.data.SupabaseBackend

class HinvrApplication : Application() {
    lateinit var sessionRepository: SessionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        sessionRepository = SessionRepository(this, SupabaseBackend())
    }
}
