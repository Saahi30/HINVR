package com.hinvr.app

import android.app.Application
import com.hinvr.app.data.CatalogRepository
import com.hinvr.app.data.SessionRepository
import com.hinvr.app.data.SupabaseBackend
import com.hinvr.app.push.ensureAartiChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HinvrApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var sessionRepository: SessionRepository
        private set
    lateinit var catalogRepository: CatalogRepository
        private set

    private val openNotificationsState = MutableStateFlow(false)
    val openNotifications: StateFlow<Boolean> = openNotificationsState.asStateFlow()

    fun requestOpenNotifications() {
        openNotificationsState.value = true
    }

    fun consumeOpenNotifications() {
        openNotificationsState.value = false
    }

    override fun onCreate() {
        super.onCreate()
        ensureAartiChannel(this)
        val backend = SupabaseBackend()
        sessionRepository = SessionRepository(this, backend)
        catalogRepository = CatalogRepository(backend)
        appScope.launch {
            sessionRepository.snapshot.collect { snap ->
                sessionRepository.noteStoredLanguage(snap.languageTag)
            }
        }
    }
}
