package com.hinvr.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the phone has a validated internet connection. The Pass screen uses it
 * to say honestly that it is showing an offline code; reconnecting retries the desk queue.
 */
class Connectivity(context: Context, private val onRegained: () -> Unit) {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val validated = mutableSetOf<Network>()
    private val _online = MutableStateFlow(currentlyOnline())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val ok = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            synchronized(validated) { if (ok) validated += network else validated -= network }
            update()
        }

        override fun onLost(network: Network) {
            synchronized(validated) { validated -= network }
            update()
        }
    }

    fun start() {
        runCatching { manager?.registerDefaultNetworkCallback(callback) }
    }

    private fun update() {
        val now = synchronized(validated) { validated.isNotEmpty() }
        val was = _online.value
        _online.value = now
        if (now && !was) onRegained()
    }

    private fun currentlyOnline(): Boolean {
        val caps = manager?.let { it.getNetworkCapabilities(it.activeNetwork) } ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
