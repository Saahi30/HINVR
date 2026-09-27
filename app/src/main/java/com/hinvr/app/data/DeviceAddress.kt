package com.hinvr.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ResolvedPlace(
    val address: String,
    val city: String,
)

class LocationException(message: String) : Exception(message)

object DeviceAddress {
    @SuppressLint("MissingPermission")
    suspend fun resolve(context: Context): ResolvedPlace = withContext(Dispatchers.IO) {
        try {
            withTimeout(15_000) { read(context.applicationContext) }
        } catch (e: LocationException) {
            throw e
        } catch (_: TimeoutCancellationException) {
            throw LocationException("Couldn’t read your location. Type the address.")
        } catch (_: SecurityException) {
            throw LocationException("Location is off. Type the address.")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun read(context: Context): ResolvedPlace {
        val manager = context.getSystemService(LocationManager::class.java)
            ?: throw LocationException("Location isn’t available on this phone.")
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) {
            throw LocationException("Turn on location, or type the address.")
        }
        var denied = false
        for (provider in providers) {
            try {
                val location = manager.getLastKnownLocation(provider) ?: currentLocation(manager, provider)
                return geocode(context, location)
            } catch (_: SecurityException) {
                denied = true
            }
        }
        if (denied) throw LocationException("Location is off. Type the address.")
        throw LocationException("Couldn’t read your location. Type the address.")
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(manager: LocationManager, provider: String): Location {
        if (Build.VERSION.SDK_INT >= 30) {
            return suspendCancellableCoroutine { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(provider, signal, Runnable::run) { location ->
                    if (!cont.isActive) return@getCurrentLocation
                    if (location != null) cont.resume(location)
                    else cont.resumeWithException(LocationException("Couldn’t read your location. Type the address."))
                }
            }
        }
        return suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }

                override fun onProviderDisabled(provider: String) {
                    manager.removeUpdates(this)
                    if (cont.isActive) {
                        cont.resumeWithException(LocationException("Turn on location, or type the address."))
                    }
                }
            }
            cont.invokeOnCancellation { manager.removeUpdates(listener) }
            manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }
    }

    private suspend fun geocode(context: Context, location: Location): ResolvedPlace {
        if (!Geocoder.isPresent()) {
            throw LocationException("Couldn’t turn that location into an address. Type it instead.")
        }
        val geocoder = Geocoder(context, Locale.getDefault())
        val found = if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            if (cont.isActive) cont.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(emptyList())
                        }
                    },
                )
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1).orEmpty()
        }
        val first = found.firstOrNull()
            ?: throw LocationException("Couldn’t turn that location into an address. Type it instead.")
        val line = first.getAddressLine(0)?.trim().orEmpty().ifBlank {
            listOfNotNull(first.thoroughfare, first.subLocality, first.locality, first.adminArea, first.postalCode)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
        }
        if (line.isBlank()) {
            throw LocationException("Couldn’t turn that location into an address. Type it instead.")
        }
        val city = first.locality?.trim().orEmpty().ifBlank { first.subAdminArea?.trim().orEmpty() }
        return ResolvedPlace(address = line, city = city)
    }
}
