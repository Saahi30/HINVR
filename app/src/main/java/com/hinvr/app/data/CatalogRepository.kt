package com.hinvr.app.data

import android.content.Context
import android.util.Log
import com.hinvr.app.ui.catalog.HinvrCatalog
import com.hinvr.app.ui.catalog.Mandir
import com.hinvr.app.ui.catalog.ServiceTile
import com.hinvr.app.ui.catalog.parseTileScene
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The desk's catalog is the truth once it has been read: its order, its
 * unpublished temples, its photos. The bundled list is only for a first launch
 * with no signal. The last good read is cached so later offline launches keep
 * the desk's version, with nothing marked live until the server says so again.
 */
class CatalogRepository(
    context: Context,
    private val backend: SupabaseBackend,
) {
    private val cacheFile = File(context.applicationContext.filesDir, "catalog_cache.json")
    private val refreshLock = Mutex()

    private val _mandirs = MutableStateFlow(HinvrCatalog.bundledMandirs)
    val mandirs: StateFlow<List<Mandir>> = _mandirs.asStateFlow()

    private val _services = MutableStateFlow(HinvrCatalog.bundledServices)
    val services: StateFlow<List<ServiceTile>> = _services.asStateFlow()

    private val _headline = MutableStateFlow(HinvrCatalog.DefaultHeadline)
    val headline: StateFlow<String> = _headline.asStateFlow()

    private var loadedFromServer = false
    private var cacheRead = false

    /** Reads the cached catalog once, then asks the server. Safe to call from any screen. */
    suspend fun refresh(): Unit = refreshLock.withLock {
        if (!cacheRead) {
            cacheRead = true
            readCache()?.let { cached -> if (!loadedFromServer) publish(cached, fromCache = true) }
        }
        val remote = backend.fetchCatalog() ?: return@withLock
        loadedFromServer = true
        publish(remote, fromCache = false)
        writeCache(remote)
    }

    /** Null when the desk has no published mandir by this id. */
    fun mandir(id: String): Mandir? {
        val current = _mandirs.value
        return current.find { it.id.equals(id, true) }
            ?: current.find { it.city.equals(id, true) }
    }

    private fun publish(remote: RemoteCatalog, fromCache: Boolean) {
        _mandirs.value = remote.mandirs.map { row ->
            val mandir = row.toMandir()
            // A cached "live" is stale by definition.
            if (fromCache) mandir.copy(live = false) else mandir
        }
        _services.value = HinvrCatalog.mergeServices(remote.services.map { it.toTile() })
        _headline.value = remote.headline?.takeIf { it.isNotBlank() } ?: HinvrCatalog.DefaultHeadline
    }

    private suspend fun readCache(): RemoteCatalog? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext null
        attempt { cacheJson.decodeFromString<RemoteCatalog>(cacheFile.readText()) }
            .onFailure { Log.w(TAG, "Catalog cache unreadable; ignoring it.", it) }
            .getOrNull()
    }

    private suspend fun writeCache(remote: RemoteCatalog): Unit = withContext(Dispatchers.IO) {
        attempt {
            val temp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
            temp.writeText(cacheJson.encodeToString(remote))
            if (!temp.renameTo(cacheFile)) {
                cacheFile.delete()
                temp.renameTo(cacheFile)
            }
        }.onFailure { Log.w(TAG, "Couldn't cache the catalog.", it) }
        Unit
    }

    private companion object {
        const val TAG = "HinvrCatalog"
        val cacheJson = Json { ignoreUnknownKeys = true }
    }
}

/**
 * Server values win. Long-form visitor text the desk has not written yet falls back
 * to the bundled copy for the same temple; live, VR, pass, links and order never do.
 */
private fun MandirRow.toMandir(): Mandir {
    val bundled = HinvrCatalog.bundledMandirs.find { it.id == id }
    fun text(server: String, fallback: String?) = server.ifBlank { fallback.orEmpty() }
    return Mandir(
        id = id,
        name = name,
        place = place,
        city = city,
        scene = parseTileScene(scene, id),
        live = live && liveUrl.isNotBlank(),
        vr = vr,
        passAccepted = passAccepted,
        nextAarti = nextAarti?.ifBlank { null },
        liveCheckedAt = liveCheckedAt?.let(::parseInstantMs),
        timings = text(timings, bundled?.timings),
        photoUrl = photoUrl.trim(),
        liveUrl = liveUrl,
        vrUrl = vrUrl,
        deity = text(deity, bundled?.deity),
        summary = text(summary, bundled?.summary),
        history = text(history, bundled?.history),
        significance = text(significance, bundled?.significance),
        architecture = text(architecture, bundled?.architecture),
        dressCode = text(dressCode, bundled?.dressCode),
        bestTime = text(bestTime, bundled?.bestTime),
        visitorNotes = text(visitorNotes, bundled?.visitorNotes),
        facilities = text(facilities, bundled?.facilities),
        address = text(address, bundled?.address),
        officialWebsite = officialWebsite,
        contactPhone = contactPhone,
    )
}

private fun ServiceRow.toTile(): ServiceTile = ServiceTile(
    title = title,
    benefit = benefit,
    scene = parseTileScene(scene, id),
    route = route,
    tall = tall,
    photoUrl = photoUrl.trim(),
)

private fun parseInstantMs(raw: String): Long? =
    runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }.getOrNull()
