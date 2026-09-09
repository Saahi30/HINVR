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
        _mandirs.value = remote.mandirs
        _services.value = remote.services
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
