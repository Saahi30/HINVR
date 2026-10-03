package com.hinvr.quest

import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class RingCard(
    val panelId: Int,
    val mandirId: String?,
    val title: String,
    val place: String,
    @DrawableRes val photo: Int?,
    val liveUrl: String = "",
    val state: String = "",
) {
    val region: String
        get() = state.ifBlank { KnownStates[mandirId].orEmpty() }.ifBlank { "Other" }
}

private val KnownStates = mapOf(
    "tirupati" to "Andhra Pradesh",
    "kashi" to "Uttar Pradesh",
    "shirdi" to "Maharashtra",
    "kedarnath" to "Uttarakhand",
    "somnath" to "Gujarat",
    "vaishno-devi" to "Jammu and Kashmir",
    "meenakshi" to "Tamil Nadu",
    "jagannath" to "Odisha",
    "dwarkadhish" to "Gujarat",
    "badrinath" to "Uttarakhand",
    "golden-temple" to "Punjab",
    "siddhivinayak" to "Maharashtra",
    "iskcon-bengaluru" to "Karnataka",
    "mahakal" to "Madhya Pradesh",
    "ram-lalla" to "Uttar Pradesh",
    "khatu-shyam" to "Rajasthan",
    "salasar" to "Rajasthan",
    "ambaji" to "Gujarat",
    "vitthal" to "Maharashtra",
    "pashupatinath" to "Nepal",
    "ashapura" to "Gujarat",
    "sarangpur" to "Gujarat",
    "mohankheda" to "Madhya Pradesh",
    "narnarayan-bhuj" to "Gujarat",
    "narnarayan-kalupur" to "Gujarat",
    "gopinathji" to "Gujarat",
    "ranchhodraiji" to "Gujarat",
    "laxmi-narayan" to "Gujarat",
    "radha-govinda" to "Telangana",
    "mahalaxmi" to "Maharashtra",
)

private val StatePhotos = mapOf(
    "Andhra Pradesh" to R.drawable.state_andhra_pradesh,
    "Uttar Pradesh" to R.drawable.state_uttar_pradesh,
    "Maharashtra" to R.drawable.state_maharashtra,
    "Uttarakhand" to R.drawable.state_uttarakhand,
    "Gujarat" to R.drawable.state_gujarat,
    "Jammu and Kashmir" to R.drawable.state_jammu_kashmir,
    "Tamil Nadu" to R.drawable.state_tamil_nadu,
    "Odisha" to R.drawable.state_odisha,
    "Punjab" to R.drawable.state_punjab,
    "Karnataka" to R.drawable.state_karnataka,
    "Madhya Pradesh" to R.drawable.state_madhya_pradesh,
    "Rajasthan" to R.drawable.state_rajasthan,
    "Nepal" to R.drawable.state_nepal,
    "Telangana" to R.drawable.state_telangana,
)

enum class Stage { Splash, Pair, Menu, States, Live, Tour, Profile, Mandir }

enum class ChoiceBadge { Live, Vr360, Sanctum }

data class HomeChoice(
    val title: String,
    val place: String,
    val badge: ChoiceBadge,
    @DrawableRes val photo: Int,
    val url: String = "",
    val localFile: String = "",
)

val MenuChoices = listOf(
    HomeChoice("Live Darshan", "THE MANDIRS", ChoiceBadge.Live, R.drawable.temple_tirupati),
    HomeChoice("Guided Tour", "THREE TEMPLES", ChoiceBadge.Vr360, R.drawable.temple_kedarnath),
    HomeChoice("Enter Mandir", "HINVR SANCTUM", ChoiceBadge.Sanctum, R.drawable.temple_kashi),
)

val TourChoices = listOf(
    HomeChoice(
        "Mahakaleshwar",
        "UJJAIN",
        ChoiceBadge.Vr360,
        R.drawable.temple_kashi,
        url = "https://www.youtube.com/embed/A5YdinFdV54",
        localFile = "mahakaleshwar.mkv",
    ),
    HomeChoice(
        "Badrinath Temple",
        "BADRINATH",
        ChoiceBadge.Vr360,
        R.drawable.temple_kedarnath,
        url = "https://www.youtube.com/embed/VDNIuBQBSmk",
        localFile = "badrinath.mkv",
    ),
    HomeChoice(
        "Ayodhya",
        "AYODHYA",
        ChoiceBadge.Vr360,
        R.drawable.temple_tirupati,
        url = "https://www.youtube.com/embed/tkCsr4LT5yQ",
        localFile = "ayodhya.mkv",
    ),
)

object Ring {
    var stage by mutableStateOf(Stage.Splash)
        private set

    var flame by mutableFloatStateOf(0.04f)
        private set

    var diyaLit by mutableStateOf(false)
        private set

    fun publishFlame(value: Float) {
        flame = value.coerceIn(0f, 1f)
    }

    fun markDiyaLit() {
        diyaLit = true
    }

    var greeting by mutableIntStateOf(0)
        private set

    var usePassthrough by mutableStateOf(false)
        private set

    fun setPassthrough(enabled: Boolean) {
        usePassthrough = enabled
    }

    fun greetAgain() {
        if (inSphere || isWatching || handedOffToYoutube) return
        if (Mandir.inside) Mandir.leave()
        stage = Stage.Splash
        index = 0
        selectedId = null
        stateName = null
        flame = 0.04f
        diyaLit = false
        greeting++
    }

    private val bundled: List<RingCard> = listOf(
        RingCard(
            R.id.ring_tirupati,
            "tirupati",
            "Sri Venkateswara",
            "TIRUPATI",
            R.drawable.temple_tirupati,
            liveUrl = "https://www.youtube.com/embed/live_stream?channel=UCTboTRX74UydvU_cBdm_cCQ",
        ),
        RingCard(
            R.id.ring_kashi,
            "kashi",
            "Kashi Vishwanath",
            "KASHI",
            R.drawable.temple_kashi,
            liveUrl = "https://www.youtube.com/embed/J0nRdUOQps8",
        ),
        RingCard(
            R.id.ring_shirdi,
            "shirdi",
            "Sai Baba",
            "SHIRDI",
            R.drawable.temple_shirdi,
            liveUrl = "https://www.youtube.com/embed/6Uf3aMujHa8",
        ),
        RingCard(R.id.ring_kedarnath, "kedarnath", "Kedarnath", "KEDARNATH", R.drawable.temple_kedarnath),
        RingCard(
            R.id.ring_somnath,
            "somnath",
            "Somnath",
            "SOMNATH",
            R.drawable.temple_somnath,
            liveUrl = "https://www.youtube.com/embed/J4z7CIrvsuw",
        ),
        RingCard(R.id.ring_vaishno_devi, "vaishno-devi", "Vaishno Devi", "KATRA", R.drawable.temple_vaishno_devi),
        RingCard(R.id.ring_meenakshi, "meenakshi", "Meenakshi Amman", "MADURAI", R.drawable.temple_meenakshi),
        RingCard(
            R.id.ring_jagannath,
            "jagannath",
            "Jagannath Temple",
            "PURI",
            R.drawable.temple_jagannath,
            liveUrl = "https://www.youtube.com/embed/oX0HEkwdWqo",
        ),
        RingCard(
            R.id.ring_dwarkadhish,
            "dwarkadhish",
            "Dwarkadhish",
            "DWARKA",
            R.drawable.temple_dwarkadhish,
            liveUrl = "https://www.youtube.com/embed/hT6FO4Qgz0c",
        ),
        RingCard(R.id.ring_badrinath, "badrinath", "Badrinath", "BADRINATH", R.drawable.temple_badrinath),
        RingCard(R.id.ring_golden_temple, "golden-temple", "Sri Harmandir Sahib", "AMRITSAR", R.drawable.temple_harmandir),
        RingCard(
            R.id.ring_siddhivinayak,
            "siddhivinayak",
            "Siddhivinayak",
            "MUMBAI",
            R.drawable.temple_siddhivinayak,
            liveUrl = "https://www.youtube.com/embed/1vGVGya72bQ",
        ),
        RingCard(R.id.ring_iskcon, "iskcon-bengaluru", "ISKCON Bengaluru", "BENGALURU", R.drawable.temple_iskcon),
        RingCard(R.id.ring_mahakal, "mahakal", "Mahakaleshwar", "UJJAIN", R.drawable.temple_mahakal, liveUrl = "https://www.youtube.com/embed/vOIx1wXmc7I"),
        RingCard(R.id.ring_ram_lalla, "ram-lalla", "Ram Lalla", "AYODHYA", R.drawable.temple_ram_lalla, liveUrl = "https://www.youtube.com/embed/xThGBtk3E_A"),
        RingCard(R.id.ring_khatu_shyam, "khatu-shyam", "Khatu Shyam", "SIKAR", R.drawable.temple_khatu_shyam, liveUrl = "https://www.youtube.com/embed/dke2IQtIfUM"),
        RingCard(R.id.ring_salasar, "salasar", "Salasar Balaji", "SALASAR", R.drawable.temple_salasar, liveUrl = "https://www.youtube.com/embed/QWVlSo719SE"),
        RingCard(R.id.ring_ambaji, "ambaji", "Ambaji", "AMBAJI", R.drawable.temple_ambaji, liveUrl = "https://www.youtube.com/embed/GQJytXfwUj0"),
        RingCard(R.id.ring_vitthal, "vitthal", "Vitthal", "PANDHARPUR", R.drawable.temple_vitthal, liveUrl = "https://www.youtube.com/embed/k5T1w4Nd-e8"),
        RingCard(R.id.ring_pashupatinath, "pashupatinath", "Pashupatinath", "KATHMANDU", R.drawable.temple_pashupatinath, liveUrl = "https://www.youtube.com/embed/eKc3whu62Hs"),
        RingCard(R.id.ring_ashapura, "ashapura", "Ashapura Mata", "LAKHPAT", R.drawable.temple_ashapura, liveUrl = "https://www.youtube.com/embed/SEQ3p6hELo8"),
        RingCard(R.id.ring_sarangpur, "sarangpur", "Kashtbhanjan Dev", "SARANGPUR", R.drawable.temple_sarangpur, liveUrl = "https://www.youtube.com/embed/KomUNKwJu6c"),
        RingCard(R.id.ring_mohankheda, "mohankheda", "Mohankheda", "DHAR", R.drawable.temple_mohankheda, liveUrl = "https://www.youtube.com/embed/gMln5rnVe6w"),
        RingCard(R.id.ring_narnarayan_bhuj, "narnarayan-bhuj", "NarNarayan Dev", "BHUJ", R.drawable.temple_narnarayan_bhuj, liveUrl = "https://www.youtube.com/embed/oJ8nu5DDRoc"),
        RingCard(R.id.ring_narnarayan_kalupur, "narnarayan-kalupur", "NarNarayan Kalupur", "AHMEDABAD", R.drawable.temple_narnarayan_kalupur, liveUrl = "https://www.youtube.com/embed/5ppU7BqHdvs"),
        RingCard(R.id.ring_gopinathji, "gopinathji", "Gopinathji", "GADHADA", R.drawable.temple_gopinathji, liveUrl = "https://www.youtube.com/embed/xWi4BUuDb7k"),
        RingCard(R.id.ring_ranchhodraiji, "ranchhodraiji", "Ranchhodraiji", "DAKOR", R.drawable.temple_ranchhodraiji, liveUrl = "https://www.youtube.com/embed/sylr5qU9uoU"),
        RingCard(R.id.ring_laxmi_narayan, "laxmi-narayan", "Laxmi Narayan", "DARSHAN", R.drawable.temple_laxmi_narayan, liveUrl = "https://www.youtube.com/embed/IIuk2Y9dqHc"),
        RingCard(R.id.ring_radha_govinda, "radha-govinda", "Radha Govinda", "HYDERABAD", R.drawable.temple_radha_govinda, liveUrl = "https://www.youtube.com/embed/TP7KeclRHcc"),
        RingCard(R.id.ring_mahalaxmi, "mahalaxmi", "Shri Mahalaxmi", "DARSHAN", R.drawable.temple_mahalaxmi, liveUrl = "https://www.youtube.com/embed/DHRoHpI_rcI"),
    )

    var mandirs by mutableStateOf(bundled)
        private set

    var stateName by mutableStateOf<String?>(null)
        private set

    val states: List<RingCard>
        get() = mandirs
            .groupBy { it.region }
            .entries
            .sortedBy { it.key.lowercase() }
            .map { (region, rows) ->
                RingCard(
                    panelId = 0,
                    mandirId = null,
                    title = region,
                    place = if (rows.size == 1) "1 MANDIR" else "${rows.size} MANDIRS",
                    photo = StatePhotos[region] ?: rows.first().photo,
                    state = region,
                )
            }

    val mandirsInState: List<RingCard>
        get() = mandirs
            .filter { it.region.equals(stateName, ignoreCase = true) }
            .sortedBy { it.title.lowercase() }

    fun replaceFromCloud(remote: List<RingCard>) {
        if (remote.isEmpty()) return
        val remoteById = remote.associateBy { it.mandirId }
        val bundledIds = bundled.mapNotNullTo(mutableSetOf()) { it.mandirId }
        mandirs = bundled.map { local ->
            val row = remoteById[local.mandirId] ?: return@map local
            local.copy(
                title = row.title.ifBlank { local.title },
                place = row.place.ifBlank { local.place },
                photo = local.photo ?: row.photo,
                liveUrl = row.liveUrl.ifBlank { local.liveUrl },
                state = row.state.ifBlank { local.region },
            )
        } + remote.filter { it.mandirId !in bundledIds }.map { row ->
            row.copy(photo = row.photo ?: R.drawable.temple_kashi)
        }
        clampIndex()
    }

    var index by mutableIntStateOf(0)
        private set

    var selectedId by mutableStateOf<String?>(null)
        private set

    var watchingUrl by mutableStateOf<String?>(null)
        private set

    var watchingTitle by mutableStateOf("")
        private set

    var watchingSpherical by mutableStateOf(false)
        private set

    var leaving by mutableStateOf(false)
        private set

    val isWatching: Boolean get() = watchingUrl != null

    var sphereTour by mutableStateOf<HomeChoice?>(null)
        private set

    var tourEnded by mutableStateOf(false)
        private set

    val inSphere: Boolean get() = sphereTour != null

    var handedOffToYoutube by mutableStateOf(false)
        private set

    fun consumeYoutubeHandoff(): Boolean {
        val away = handedOffToYoutube
        handedOffToYoutube = false
        return away
    }

    fun markTourEnded() {
        if (inSphere) tourEnded = true
    }

    fun watchTourAgain() {
        if (!inSphere) return
        tourEnded = false
        SpherePlayer.replay()
    }

    fun leaveSphere() {
        if (!inSphere) return
        SpherePlayer.stop()
        sphereTour = null
        tourEnded = false
        openMenu()
    }

    val deckSize: Int
        get() = when (stage) {
            Stage.Splash, Stage.Pair, Stage.Profile, Stage.Mandir -> 1
            Stage.Menu -> MenuChoices.size
            Stage.States -> states.size
            Stage.Live -> mandirsInState.size
            Stage.Tour -> TourChoices.size
        }

    fun scroll(delta: Int) {
        val size = deckSize
        if (delta == 0 || isWatching || !showsCards || size == 0) return
        index = (index + delta).floorMod(size)
        selectedId = null
    }

    fun lookAt(cardIndex: Int) {
        if (isWatching || !showsCards || deckSize == 0) return
        val wrapped = cardIndex.floorMod(deckSize)
        if (index == wrapped) return
        index = wrapped
        selectedId = null
    }

    fun focus(cardIndex: Int) {
        if (isWatching || !showsCards || deckSize == 0) return
        val wrapped = cardIndex.floorMod(deckSize)
        if (stage == Stage.Splash || stage == Stage.Menu || stage == Stage.Tour) {
            index = wrapped
            selectCentered()
            return
        }
        if (wrapped == index) {
            selectCentered()
        } else {
            index = wrapped
            selectedId = null
        }
    }

    fun selectCentered() {
        if (isWatching) return
        when (stage) {
            Stage.Splash -> openMenu()
            Stage.Pair, Stage.Profile, Stage.Mandir -> Unit
            Stage.Menu -> when (index) {
                0 -> openStates()
                1 -> openTour()
                else -> openMandir()
            }
            Stage.States -> {
                val card = states.getOrNull(index) ?: return
                stateName = card.state
                stage = Stage.Live
                index = 0
                selectedId = null
            }
            Stage.Live -> {
                val card = mandirsInState.getOrNull(index) ?: return
                selectedId = card.mandirId
                watch(card.liveUrl, card.title, spherical = false)
            }
            Stage.Tour -> {
                val tour = TourChoices[index]
                when {
                    SpherePlayer.play(tour) -> {
                        sphereTour = tour
                        tourEnded = false
                    }
                    Tour360.open(tour.url) -> handedOffToYoutube = true
                    else -> watch(tour.url, tour.title, spherical = true)
                }
            }
        }
    }

    /** Past the greeting only once this headset is paired with a member. */
    fun openMenu() {
        if (!QuestAccount.isPaired) {
            stage = Stage.Pair
            index = 0
            selectedId = null
            if (flame < 0.85f) flame = 1f
            return
        }
        stage = Stage.Menu
        index = 0
        selectedId = null
        stateName = null
        if (flame < 0.85f) flame = 1f
    }

    fun back() {
        if (inSphere) {
            leaveSphere()
            return
        }
        if (isWatching) {
            leaveStream()
            return
        }
        when (stage) {
            Stage.Splash -> openMenu()
            Stage.Pair, Stage.Menu -> Unit
            Stage.States, Stage.Tour, Stage.Profile, Stage.Mandir -> {
                if (stage == Stage.Mandir) Mandir.leave()
                openMenu()
            }
            Stage.Live -> {
                val name = stateName
                stage = Stage.States
                index = states.indexOfFirst { it.state == name }.coerceAtLeast(0)
                selectedId = null
            }
        }
    }

    val showsCards: Boolean
        get() = stage == Stage.Menu || stage == Stage.States || stage == Stage.Live || stage == Stage.Tour

    fun openProfile() {
        if (!QuestAccount.isPaired || isWatching || inSphere || Mandir.inside) return
        stage = Stage.Profile
        index = 0
        selectedId = null
    }

    /** The link is gone: stop whatever is playing and wait for a new pairing. */
    fun requirePairing() {
        if (inSphere) {
            SpherePlayer.stop()
            sphereTour = null
            tourEnded = false
        }
        if (isWatching) finishLeave()
        if (Mandir.inside) Mandir.leave()
        if (stage == Stage.Splash) return
        stage = Stage.Pair
        index = 0
        selectedId = null
    }

    fun leaveStream() {
        if (!isWatching || leaving) return
        leaving = true
    }

    fun finishLeave() {
        watchingUrl = null
        watchingTitle = ""
        watchingSpherical = false
        leaving = false
    }

    private fun openStates() {
        QuestCatalog.refresh()
        stage = Stage.States
        index = 0
        selectedId = null
        stateName = null
    }

    private fun clampIndex() {
        val size = when (stage) {
            Stage.States -> states.size
            Stage.Live -> mandirsInState.size
            else -> return
        }
        if (size == 0) {
            if (stage == Stage.Live) {
                stage = Stage.States
                index = 0
            }
            return
        }
        if (index >= size) index = 0
    }

    private fun openTour() {
        stage = Stage.Tour
        index = 0
        selectedId = null
    }

    private fun openMandir() {
        QuestCatalog.refresh()
        stage = Stage.Mandir
        index = 0
        selectedId = null
        Mandir.enter()
        MandirScene.show()
    }

    private fun watch(url: String, title: String, spherical: Boolean) {
        watchingUrl = url
        watchingTitle = title
        watchingSpherical = spherical
    }
}

object RingSlots {
    const val COUNT = 16
    val shown = mutableStateListOf<Int>().apply { repeat(COUNT) { add(-1) } }
}

private fun Int.floorMod(size: Int): Int = ((this % size) + size) % size
