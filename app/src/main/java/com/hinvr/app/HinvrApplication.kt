package com.hinvr.app

import android.app.Application
import com.hinvr.app.data.CatalogRepository
import com.hinvr.app.data.Connectivity
import com.hinvr.app.data.SessionRepository
import com.hinvr.app.data.SupabaseBackend
import com.hinvr.app.push.ensureAartiChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HinvrApplication : Application() {
    /** Work that must outlive a screen, like the launch sync the splash starts. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var sessionRepository: SessionRepository
        private set
    lateinit var catalogRepository: CatalogRepository
        private set
    lateinit var connectivity: Connectivity
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
        catalogRepository = CatalogRepository(this, backend)
        connectivity = Connectivity(this) { sessionRepository.onNetworkAvailable() }
        connectivity.start()
        appScope.launch {
            sessionRepository.snapshot
                .filter { it.loaded }
                .map { it.languageTag }
                .distinctUntilChanged()
                .collect { tag -> sessionRepository.noteStoredLanguage(tag) }
        }
    }
}
