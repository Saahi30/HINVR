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
)

enum class Stage { Splash, Menu, Live, Tour }

enum class ChoiceBadge { Live, Vr360 }

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
)

val TourChoices = listOf(
    HomeChoice(
        "Mahakaleshwar",
        "UJJAIN",
        ChoiceBadge.Vr360,
        R.drawable.temple_kashi,
        url = "https://www.youtube.com/embed/A5YdinFdV54",
    ),
    HomeChoice(
        "Badrinath Temple",
        "BADRINATH",
        ChoiceBadge.Vr360,
        R.drawable.temple_kedarnath,
        url = "https://www.youtube.com/embed/VDNIuBQBSmk",
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
        if (inSphere || isWatching) return
        stage = Stage.Splash
        index = 0
        selectedId = null
        flame = 0.04f
        diyaLit = false
        greeting++
    }

    val cards: List<RingCard> = listOf(
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
        RingCard(R.id.ring_vaishno_devi, "vaishno-devi", "Vaishno Devi", "KATRA", R.drawable.temple_kedarnath),
        RingCard(R.id.ring_meenakshi, "meenakshi", "Meenakshi Amman", "MADURAI", R.drawable.temple_tirupati),
        RingCard(
            R.id.ring_jagannath,
            "jagannath",
            "Jagannath Temple",
            "PURI",
            R.drawable.temple_somnath,
            liveUrl = "https://www.youtube.com/embed/oX0HEkwdWqo",
        ),
        RingCard(
            R.id.ring_dwarkadhish,
            "dwarkadhish",
            "Dwarkadhish",
            "DWARKA",
            R.drawable.temple_somnath,
            liveUrl = "https://www.youtube.com/embed/hT6FO4Qgz0c",
        ),
        RingCard(R.id.ring_badrinath, "badrinath", "Badrinath", "BADRINATH", R.drawable.temple_kedarnath),
        RingCard(R.id.ring_golden_temple, "golden-temple", "Sri Harmandir Sahib", "AMRITSAR", R.drawable.temple_kashi),
        RingCard(
            R.id.ring_siddhivinayak,
            "siddhivinayak",
            "Siddhivinayak",
            "MUMBAI",
            R.drawable.temple_shirdi,
            liveUrl = "https://www.youtube.com/embed/1vGVGya72bQ",
        ),
        RingCard(R.id.ring_iskcon, "iskcon-bengaluru", "ISKCON Bengaluru", "BENGALURU", R.drawable.temple_tirupati),
        RingCard(R.id.ring_mahakal, "mahakal", "Mahakaleshwar", "UJJAIN", R.drawable.temple_kashi, liveUrl = "https://www.youtube.com/embed/vOIx1wXmc7I"),
        RingCard(R.id.ring_ram_lalla, "ram-lalla", "Ram Lalla", "AYODHYA", R.drawable.temple_kashi, liveUrl = "https://www.youtube.com/embed/xThGBtk3E_A"),
        RingCard(R.id.ring_khatu_shyam, "khatu-shyam", "Khatu Shyam", "SIKAR", R.drawable.temple_kedarnath, liveUrl = "https://www.youtube.com/embed/dke2IQtIfUM"),
        RingCard(R.id.ring_salasar, "salasar", "Salasar Balaji", "SALASAR", R.drawable.temple_kedarnath, liveUrl = "https://www.youtube.com/embed/QWVlSo719SE"),
        RingCard(R.id.ring_ambaji, "ambaji", "Ambaji", "AMBAJI", R.drawable.temple_kedarnath, liveUrl = "https://www.youtube.com/embed/GQJytXfwUj0"),
        RingCard(R.id.ring_vitthal, "vitthal", "Vitthal", "PANDHARPUR", R.drawable.temple_shirdi, liveUrl = "https://www.youtube.com/embed/k5T1w4Nd-e8"),
        RingCard(R.id.ring_pashupatinath, "pashupatinath", "Pashupatinath", "KATHMANDU", R.drawable.temple_kashi, liveUrl = "https://www.youtube.com/embed/eKc3whu62Hs"),
        RingCard(R.id.ring_ashapura, "ashapura", "Ashapura Mata", "LAKHPAT", R.drawable.temple_somnath, liveUrl = "https://www.youtube.com/embed/SEQ3p6hELo8"),
        RingCard(R.id.ring_sarangpur, "sarangpur", "Kashtbhanjan Dev", "SARANGPUR", R.drawable.temple_shirdi, liveUrl = "https://www.youtube.com/embed/KomUNKwJu6c"),
        RingCard(R.id.ring_mohankheda, "mohankheda", "Mohankheda", "DHAR", R.drawable.temple_somnath, liveUrl = "https://www.youtube.com/embed/gMln5rnVe6w"),
        RingCard(R.id.ring_narnarayan_bhuj, "narnarayan-bhuj", "NarNarayan Dev", "BHUJ", R.drawable.temple_somnath, liveUrl = "https://www.youtube.com/embed/oJ8nu5DDRoc"),
        RingCard(R.id.ring_narnarayan_kalupur, "narnarayan-kalupur", "NarNarayan Kalupur", "AHMEDABAD", R.drawable.temple_somnath, liveUrl = "https://www.youtube.com/embed/5ppU7BqHdvs"),
        RingCard(R.id.ring_gopinathji, "gopinathji", "Gopinathji", "GADHADA", R.drawable.temple_somnath, liveUrl = "https://www.youtube.com/embed/xWi4BUuDb7k"),
        RingCard(R.id.ring_ranchhodraiji, "ranchhodraiji", "Ranchhodraiji", "DAKOR", R.drawable.temple_somnath, liveUrl = "https://www.youtube.com/embed/sylr5qU9uoU"),
        RingCard(R.id.ring_laxmi_narayan, "laxmi-narayan", "Laxmi Narayan", "DARSHAN", R.drawable.temple_tirupati, liveUrl = "https://www.youtube.com/embed/IIuk2Y9dqHc"),
        RingCard(R.id.ring_radha_govinda, "radha-govinda", "Radha Govinda", "HYDERABAD", R.drawable.temple_tirupati, liveUrl = "https://www.youtube.com/embed/TP7KeclRHcc"),
        RingCard(R.id.ring_mahalaxmi, "mahalaxmi", "Shri Mahalaxmi", "DARSHAN", R.drawable.temple_tirupati, liveUrl = "https://www.youtube.com/embed/DHRoHpI_rcI"),
    )

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

    val isWatching: Boolean get() = !watchingUrl.isNullOrBlank()

    var sphereTour by mutableStateOf<HomeChoice?>(null)
        private set

    var tourEnded by mutableStateOf(false)
        private set

    val inSphere: Boolean get() = sphereTour != null

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
            Stage.Splash -> 1
            Stage.Menu -> MenuChoices.size
            Stage.Live -> cards.size
            Stage.Tour -> TourChoices.size
        }

    fun scroll(delta: Int) {
        if (delta == 0 || isWatching || stage == Stage.Splash) return
        index = (index + delta).floorMod(deckSize)
        selectedId = null
    }

    fun focus(cardIndex: Int) {
        if (isWatching) return
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
            Stage.Menu -> if (index == 0) openLive() else openTour()
            Stage.Live -> {
                val card = cards[index]
                selectedId = card.mandirId
                if (card.liveUrl.isNotBlank()) watch(card.liveUrl, card.title, spherical = false)
            }
            Stage.Tour -> {
                val tour = TourChoices[index]
                when {
                    SpherePlayer.play(tour) -> {
                        sphereTour = tour
                        tourEnded = false
                    }
                    Tour360.open(tour.url) -> Unit
                    else -> watch(tour.url, tour.title, spherical = true)
                }
            }
        }
    }

    fun openMenu() {
        stage = Stage.Menu
        index = 0
        selectedId = null
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
            Stage.Menu -> Unit
            Stage.Live, Stage.Tour -> openMenu()
        }
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

    private fun openLive() {
        stage = Stage.Live
        index = 0
        selectedId = null
    }

    private fun openTour() {
        stage = Stage.Tour
        index = 1
        selectedId = null
    }

    private fun watch(url: String, title: String, spherical: Boolean) {
        watchingUrl = url
        watchingTitle = title
        watchingSpherical = spherical
    }
}

object RingSlots {
    const val COUNT = 5
    val shown = mutableStateListOf(-1, -1, -1, -1, -1)
}

private fun Int.floorMod(size: Int): Int = ((this % size) + size) % size
