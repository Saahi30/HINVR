package com.hinvr.app

import android.app.Application
import com.hinvr.app.data.CatalogRepository
import com.hinvr.app.data.SessionRepository
import com.hinvr.app.data.SupabaseBackend

class HinvrApplication : Application() {
    lateinit var sessionRepository: SessionRepository
        private set
    lateinit var catalogRepository: CatalogRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val backend = SupabaseBackend()
        sessionRepository = SessionRepository(this, backend)
        catalogRepository = CatalogRepository(backend)
    }
}
