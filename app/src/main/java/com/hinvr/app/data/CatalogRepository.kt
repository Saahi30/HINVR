package com.hinvr.app.data

import com.hinvr.app.ui.catalog.HinvrCatalog
import com.hinvr.app.ui.catalog.Mandir
import com.hinvr.app.ui.catalog.ServiceTile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CatalogRepository(
    private val backend: SupabaseBackend,
) {
    private val _mandirs = MutableStateFlow(HinvrCatalog.bundledMandirs)
    val mandirs: StateFlow<List<Mandir>> = _mandirs.asStateFlow()

    private val _services = MutableStateFlow(HinvrCatalog.bundledServices)
    val services: StateFlow<List<ServiceTile>> = _services.asStateFlow()

    private val _headline = MutableStateFlow(HinvrCatalog.DefaultHeadline)
    val headline: StateFlow<String> = _headline.asStateFlow()

    suspend fun refresh() {
        val remote = backend.fetchCatalog() ?: return
        if (remote.mandirs.isNotEmpty()) {
            val remoteById = remote.mandirs.associateBy { it.id }
            val bundledIds = HinvrCatalog.bundledMandirs.mapTo(mutableSetOf()) { it.id }
            _mandirs.value = HinvrCatalog.bundledMandirs.map { bundled ->
                remoteById[bundled.id]?.let { row ->
                    row.copy(
                        photoUrl = row.photoUrl.ifBlank { bundled.photoUrl },
                        liveUrl = row.liveUrl.ifBlank { bundled.liveUrl },
                        vrUrl = row.vrUrl.ifBlank { bundled.vrUrl },
                        deity = row.deity.ifBlank { bundled.deity },
                        summary = row.summary.ifBlank { bundled.summary },
                        history = row.history.ifBlank { bundled.history },
                        significance = row.significance.ifBlank { bundled.significance },
                        architecture = row.architecture.ifBlank { bundled.architecture },
                        dressCode = row.dressCode.ifBlank { bundled.dressCode },
                        bestTime = row.bestTime.ifBlank { bundled.bestTime },
                        visitorNotes = row.visitorNotes.ifBlank { bundled.visitorNotes },
                        facilities = row.facilities.ifBlank { bundled.facilities },
                        address = row.address.ifBlank { bundled.address },
                        officialWebsite = row.officialWebsite.ifBlank { bundled.officialWebsite },
                        contactPhone = row.contactPhone.ifBlank { bundled.contactPhone },
                    )
                } ?: bundled
            } + remote.mandirs.filterNot { it.id in bundledIds }
        }
        _services.value = HinvrCatalog.mergeServices(remote.services)
        remote.headline?.takeIf { it.isNotBlank() }?.let { _headline.value = it }
    }

    fun mandir(id: String): Mandir {
        val current = _mandirs.value
        return current.find { it.id.equals(id, true) || it.city.equals(id, true) }
            ?: HinvrCatalog.bundledMandirs.find { it.id.equals(id, true) }
            ?: current.firstOrNull()
            ?: HinvrCatalog.bundledMandirs.first()
    }
}
