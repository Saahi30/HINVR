package com.hinvr.quest

import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** What the server's live check last said about a mandir's stream. */
enum class LiveState { Live, NotLive, Unknown }

data class RingCard(
    val mandirId: String?,
    val title: String,
    val place: String,
    @DrawableRes val photo: Int?,
    val liveUrl: String = "",
    val live: LiveState = LiveState.Unknown,
    val state: String = "",
) {
    val region: String
        get() = state.ifBlank { KnownStates[mandirId].orEmpty() }.ifBlank { "Other" }

    val hasStream: Boolean get() = liveUrl.isNotBlank()
}

internal val KnownStates = mapOf(
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

enum class ChoiceBadge { Live, Vr360, Sanctum }

data class MenuChoice(
    @StringRes val title: Int,
    @StringRes val place: Int,
    val badge: ChoiceBadge,
    @DrawableRes val photo: Int,
)

/** A recorded 360 walk. Never live. */
data class HomeChoice(
    val title: String,
    val place: String,
    @DrawableRes val photo: Int,
    val url: String = "",
    val localFile: String = "",
)

val MenuChoices = listOf(
    MenuChoice(R.string.menu_live, R.string.menu_live_place, ChoiceBadge.Live, R.drawable.temple_tirupati),
    MenuChoice(R.string.menu_tour, R.string.menu_tour_place, ChoiceBadge.Vr360, R.drawable.temple_kedarnath),
    MenuChoice(R.string.menu_mandir, R.string.menu_mandir_place, ChoiceBadge.Sanctum, R.drawable.sanctum_panorama),
)

val TourChoices = listOf(
    HomeChoice(
        "Mahakaleshwar",
        "UJJAIN",
        R.drawable.temple_mahakal,
        url = "https://www.youtube.com/embed/A5YdinFdV54",
        localFile = "mahakaleshwar.mkv",
    ),
    HomeChoice(
        "Badrinath Temple",
        "BADRINATH",
        R.drawable.temple_badrinath,
        url = "https://www.youtube.com/embed/VDNIuBQBSmk",
        localFile = "badrinath.mkv",
    ),
    HomeChoice(
        "Ayodhya",
        "AYODHYA",
        R.drawable.temple_ram_lalla,
        url = "https://www.youtube.com/embed/tkCsr4LT5yQ",
        localFile = "ayodhya.mkv",
    ),
)

/** Offline fallback only. Once the server answers, its catalog is the catalog. */
val BundledMandirs: List<RingCard> = listOf(
    RingCard(
        "tirupati",
        "Sri Venkateswara",
        "TIRUPATI",
        R.drawable.temple_tirupati,
        liveUrl = "https://www.youtube.com/embed/live_stream?channel=UCTboTRX74UydvU_cBdm_cCQ",
    ),
    RingCard("kashi", "Kashi Vishwanath", "KASHI", R.drawable.temple_kashi, liveUrl = "https://www.youtube.com/embed/J0nRdUOQps8"),
    RingCard("shirdi", "Sai Baba", "SHIRDI", R.drawable.temple_shirdi, liveUrl = "https://www.youtube.com/embed/6Uf3aMujHa8"),
    RingCard("kedarnath", "Kedarnath", "KEDARNATH", R.drawable.temple_kedarnath),
    RingCard("somnath", "Somnath", "SOMNATH", R.drawable.temple_somnath, liveUrl = "https://www.youtube.com/embed/J4z7CIrvsuw"),
    RingCard("vaishno-devi", "Vaishno Devi", "KATRA", R.drawable.temple_vaishno_devi),
    RingCard("meenakshi", "Meenakshi Amman", "MADURAI", R.drawable.temple_meenakshi),
    RingCard("jagannath", "Jagannath Temple", "PURI", R.drawable.temple_jagannath, liveUrl = "https://www.youtube.com/embed/oX0HEkwdWqo"),
    RingCard("dwarkadhish", "Dwarkadhish", "DWARKA", R.drawable.temple_dwarkadhish, liveUrl = "https://www.youtube.com/embed/hT6FO4Qgz0c"),
    RingCard("badrinath", "Badrinath", "BADRINATH", R.drawable.temple_badrinath),
    RingCard("golden-temple", "Sri Harmandir Sahib", "AMRITSAR", R.drawable.temple_harmandir),
    RingCard("siddhivinayak", "Siddhivinayak", "MUMBAI", R.drawable.temple_siddhivinayak, liveUrl = "https://www.youtube.com/embed/1vGVGya72bQ"),
    RingCard("iskcon-bengaluru", "ISKCON Bengaluru", "BENGALURU", R.drawable.temple_iskcon),
    RingCard("mahakal", "Mahakaleshwar", "UJJAIN", R.drawable.temple_mahakal, liveUrl = "https://www.youtube.com/embed/vOIx1wXmc7I"),
    RingCard("ram-lalla", "Ram Lalla", "AYODHYA", R.drawable.temple_ram_lalla, liveUrl = "https://www.youtube.com/embed/xThGBtk3E_A"),
    RingCard("khatu-shyam", "Khatu Shyam", "SIKAR", R.drawable.temple_khatu_shyam, liveUrl = "https://www.youtube.com/embed/dke2IQtIfUM"),
    RingCard("salasar", "Salasar Balaji", "SALASAR", R.drawable.temple_salasar, liveUrl = "https://www.youtube.com/embed/QWVlSo719SE"),
    RingCard("ambaji", "Ambaji", "AMBAJI", R.drawable.temple_ambaji, liveUrl = "https://www.youtube.com/embed/GQJytXfwUj0"),
    RingCard("vitthal", "Vitthal", "PANDHARPUR", R.drawable.temple_vitthal, liveUrl = "https://www.youtube.com/embed/k5T1w4Nd-e8"),
    RingCard("pashupatinath", "Pashupatinath", "KATHMANDU", R.drawable.temple_pashupatinath, liveUrl = "https://www.youtube.com/embed/eKc3whu62Hs"),
    RingCard("ashapura", "Ashapura Mata", "LAKHPAT", R.drawable.temple_ashapura, liveUrl = "https://www.youtube.com/embed/SEQ3p6hELo8"),
    RingCard("sarangpur", "Kashtbhanjan Dev", "SARANGPUR", R.drawable.temple_sarangpur, liveUrl = "https://www.youtube.com/embed/KomUNKwJu6c"),
    RingCard("mohankheda", "Mohankheda", "DHAR", R.drawable.temple_mohankheda, liveUrl = "https://www.youtube.com/embed/gMln5rnVe6w"),
    RingCard("narnarayan-bhuj", "NarNarayan Dev", "BHUJ", R.drawable.temple_narnarayan_bhuj, liveUrl = "https://www.youtube.com/embed/oJ8nu5DDRoc"),
    RingCard("narnarayan-kalupur", "NarNarayan Kalupur", "AHMEDABAD", R.drawable.temple_narnarayan_kalupur, liveUrl = "https://www.youtube.com/embed/5ppU7BqHdvs"),
    RingCard("gopinathji", "Gopinathji", "GADHADA", R.drawable.temple_gopinathji, liveUrl = "https://www.youtube.com/embed/xWi4BUuDb7k"),
    RingCard("ranchhodraiji", "Ranchhodraiji", "DAKOR", R.drawable.temple_ranchhodraiji, liveUrl = "https://www.youtube.com/embed/sylr5qU9uoU"),
    RingCard("laxmi-narayan", "Laxmi Narayan", "DARSHAN", R.drawable.temple_laxmi_narayan, liveUrl = "https://www.youtube.com/embed/IIuk2Y9dqHc"),
    RingCard("radha-govinda", "Radha Govinda", "HYDERABAD", R.drawable.temple_radha_govinda, liveUrl = "https://www.youtube.com/embed/TP7KeclRHcc"),
    RingCard("mahalaxmi", "Shri Mahalaxmi", "DARSHAN", R.drawable.temple_mahalaxmi, liveUrl = "https://www.youtube.com/embed/DHRoHpI_rcI"),
)

/** Pure catalog rules, kept apart from Compose state so they can be unit tested. */
object CatalogRules {
    /** The cron checks every 10 minutes; a "live" older than this is no longer a claim we repeat. */
    const val LiveFreshMs = 30 * 60_000L

    fun liveState(live: Boolean, checkedAtMs: Long?, nowMs: Long): LiveState = when {
        !live -> LiveState.NotLive
        // Staff set the flag by hand for mandirs without a checked feed.
        checkedAtMs == null -> LiveState.Live
        nowMs - checkedAtMs <= LiveFreshMs -> LiveState.Live
        else -> LiveState.Unknown
    }

    /**
     * The server's list wins: its order, its streams (a blank stream means no stream), and only
     * what it publishes. Bundled rows only lend their licensed card photo and a state name.
     */
    fun merge(bundled: List<RingCard>, remote: List<RingCard>): List<RingCard> {
        val local = bundled.associateBy { it.mandirId }
        return remote.filter { !it.mandirId.isNullOrBlank() }.distinctBy { it.mandirId }.map { row ->
            val known = local[row.mandirId]
            row.copy(
                title = row.title.ifBlank { known?.title.orEmpty() },
                place = row.place.ifBlank { known?.place.orEmpty() },
                photo = row.photo ?: known?.photo,
                state = row.state.ifBlank { known?.region ?: KnownStates[row.mandirId].orEmpty() },
            )
        }
    }

    fun states(mandirs: List<RingCard>): List<RingCard> = mandirs
        .groupBy { it.region }
        .entries
        .sortedBy { it.key.lowercase() }
        .map { (region, rows) ->
            RingCard(
                mandirId = null,
                title = region,
                place = if (rows.size == 1) "1 MANDIR" else "${rows.size} MANDIRS",
                photo = StatePhotos[region] ?: rows.firstNotNullOfOrNull { it.photo },
                state = region,
                live = if (rows.any { it.live == LiveState.Live }) LiveState.Live else LiveState.Unknown,
            )
        }

    fun inState(mandirs: List<RingCard>, state: String?): List<RingCard> = mandirs
        .filter { it.region.equals(state, ignoreCase = true) }
        .sortedBy { it.title.lowercase() }

    /** Jyotirlinga streams first, since the sanctum's murti is a Shiva lingam. Live beats not live. */
    private val ShivaFirst = listOf("kashi", "somnath", "mahakal")

    fun defaultDarshan(choices: List<RingCard>): RingCard? {
        val shiva = choices.filter { it.mandirId in ShivaFirst }.sortedBy { ShivaFirst.indexOf(it.mandirId) }
        return shiva.firstOrNull { it.live == LiveState.Live }
            ?: shiva.firstOrNull()
            ?: choices.firstOrNull { it.live == LiveState.Live }
            ?: choices.firstOrNull()
    }
}

object Ring {
    var nav by mutableStateOf(NavState())
        private set

    val stage: Stage get() = nav.stage
    val index: Int get() = nav.index
    val selectedId: String? get() = nav.selectedId
    val stateName: String? get() = nav.stateName
    val showsCards: Boolean get() = nav.showsCards

    val stream: StreamSource? get() = (nav.overlay as? Overlay.Stream)?.source
    val isWatching: Boolean get() = nav.overlay is Overlay.Stream
    val leaving: Boolean get() = (nav.overlay as? Overlay.Stream)?.leaving == true
    val sphere: Overlay.Sphere? get() = nav.overlay as? Overlay.Sphere
    val inSphere: Boolean get() = nav.overlay is Overlay.Sphere

    var flame by mutableFloatStateOf(0.04f)
        private set

    var diyaLit by mutableStateOf(false)
        private set

    var greeting by mutableIntStateOf(0)
        private set

    /** When the opening first reached the member's eyes, in elapsedRealtime. 0 until then. */
    var splashShownAtMs by mutableLongStateOf(0L)
        private set

    val splashShown: Boolean get() = splashShownAtMs != 0L

    var usePassthrough by mutableStateOf(false)
        private set

    /** Bumped when the floor was recentred, so head-anchored panels re-anchor. */
    var recenters by mutableIntStateOf(0)
        private set

    fun publishFlame(value: Float) {
        flame = value.coerceIn(0f, 1f)
    }

    fun markDiyaLit() {
        if (diyaLit) return
        diyaLit = true
        SanctumSound.welcome()
    }

    fun markSplashShown() {
        if (stage == Stage.Splash && splashShownAtMs == 0L) splashShownAtMs = SystemClock.elapsedRealtime()
    }

    fun setPassthrough(enabled: Boolean) {
        usePassthrough = enabled
    }

    fun recentered() {
        recenters++
    }

    // --- Catalog ---

    var mandirs by mutableStateOf(BundledMandirs)
        private set

    /** True once the server's catalog replaced the bundled one. */
    var fromServer by mutableStateOf(false)
        private set

    var states by mutableStateOf(CatalogRules.states(BundledMandirs))
        private set

    private var inStateKey: Pair<List<RingCard>, String?>? = null
    private var inStateRows: List<RingCard> = emptyList()

    val mandirsInState: List<RingCard>
        get() {
            val key = mandirs to stateName
            if (inStateKey?.first !== key.first || inStateKey?.second != key.second) {
                inStateKey = key
                inStateRows = CatalogRules.inState(mandirs, stateName)
            }
            return inStateRows
        }

    fun replaceFromCloud(remote: List<RingCard>) {
        mandirs = CatalogRules.merge(BundledMandirs, remote)
        states = CatalogRules.states(mandirs)
        fromServer = true
        clampIndex()
        Mandir.catalogChanged()
    }

    val deckSize: Int
        get() = when (stage) {
            Stage.Splash, Stage.Pair, Stage.Credits, Stage.Profile, Stage.Mandir -> 1
            Stage.Menu -> MenuChoices.size
            Stage.States -> states.size
            Stage.Live -> mandirsInState.size
            Stage.Tour -> TourChoices.size
        }

    // --- Navigation ---

    /** Every screen change goes through here, so leaving a screen always cleans up after it. */
    private fun go(next: NavState) {
        val prev = nav
        if (prev == next) return
        nav = next
        val prevSphere = prev.overlay as? Overlay.Sphere
        val nextSphere = next.overlay as? Overlay.Sphere
        if (prevSphere != null && nextSphere?.tour != prevSphere.tour) SpherePlayer.stop()
        if (prev.stage == Stage.Mandir && next.stage != Stage.Mandir) Mandir.exit()
        if (next.stage == Stage.Mandir && prev.stage != Stage.Mandir) Mandir.enter()
        if (next.stage == Stage.Splash && prev.stage != Stage.Splash) {
            flame = 0.04f
            diyaLit = false
            splashShownAtMs = 0L
            greeting++
        }
        if (prev.stage == Stage.Splash && next.stage != Stage.Splash) {
            splashShownAtMs = 0L
            if (flame < 0.85f) flame = 1f
        }
    }

    fun greetAgain() = go(Nav.greet(nav))

    /** Past the greeting only once this headset is paired with a member. */
    fun openMenu() = go(Nav.menu(nav, QuestAccount.isPaired))

    fun endSplash() {
        if (stage == Stage.Splash) openMenu()
    }

    fun back() {
        val parent = if (stage == Stage.Live) states.indexOfFirst { it.state == stateName }.coerceAtLeast(0) else 0
        go(Nav.back(nav, QuestAccount.isPaired, parent))
    }

    fun openProfile() = go(Nav.profile(nav, QuestAccount.isPaired))

    fun openCredits() = go(Nav.credits(nav))

    /** The link is gone: stop whatever is playing and wait for a new pairing. */
    fun requirePairing() = go(Nav.requirePairing(nav))

    fun leaveStream() = go(Nav.back(nav, QuestAccount.isPaired))

    fun finishLeave() = go(Nav.finishLeave(nav))

    fun consumeYoutubeHandoff(): Boolean {
        val away = nav.overlay == Overlay.Handoff
        go(Nav.returnFromHandoff(nav))
        return away
    }

    fun sphereStatus(status: SphereStatus) = go(Nav.sphereStatus(nav, status))

    fun watchTourAgain() {
        val tour = sphere ?: return
        go(Nav.sphereStatus(nav, SphereStatus.Loading))
        if (tour.status == SphereStatus.Failed) SpherePlayer.play(tour.tour) else SpherePlayer.replay()
    }

    /** "Back to menu" from the end of a tour. */
    fun leaveSphere() {
        if (!inSphere) return
        openMenu()
    }

    fun openMandir(explore: Boolean = false) {
        QuestCatalog.refresh()
        go(Nav.mandir(nav, QuestAccount.isPaired))
        if (explore && stage == Stage.Mandir) Mandir.beginVisit()
    }

    fun scroll(delta: Int) {
        val size = deckSize
        if (delta == 0 || !showsCards || nav.overlay != Overlay.None || size == 0) return
        nav = nav.copy(index = (index + delta).floorMod(size), selectedId = null)
    }

    fun lookAt(cardIndex: Int) {
        val size = deckSize
        if (!showsCards || nav.overlay != Overlay.None || size == 0) return
        val wrapped = cardIndex.floorMod(size)
        if (index == wrapped) return
        nav = nav.copy(index = wrapped, selectedId = null)
    }

    /** A click on a card. Menu and tour cards open at once; mandir cards centre first. */
    fun focus(cardIndex: Int) {
        val size = deckSize
        if (!showsCards || nav.overlay != Overlay.None || size == 0) return
        val wrapped = cardIndex.floorMod(size)
        if (stage == Stage.Menu || stage == Stage.Tour || wrapped == index) {
            nav = nav.copy(index = wrapped)
            selectCentered()
        } else {
            nav = nav.copy(index = wrapped, selectedId = null)
        }
    }

    private fun selectCentered() {
        when (stage) {
            Stage.Splash, Stage.Pair, Stage.Credits, Stage.Profile, Stage.Mandir -> Unit
            Stage.Menu -> when (index) {
                0 -> {
                    QuestCatalog.refresh()
                    go(Nav.openStates(nav))
                }
                1 -> go(Nav.openTour(nav))
                else -> openMandir()
            }
            Stage.States -> {
                val card = states.getOrNull(index) ?: return
                go(Nav.openState(nav, card.state))
            }
            Stage.Live -> {
                val card = mandirsInState.getOrNull(index) ?: return
                nav = nav.copy(selectedId = card.mandirId)
                // A mandir without a stream says so on its card; there is nothing to open.
                if (!card.hasStream) return
                go(Nav.watch(nav, StreamSource(card.liveUrl, card.title, spherical = false, live = card.live)))
            }
            Stage.Tour -> {
                val tour = TourChoices.getOrNull(index) ?: return
                when {
                    SpherePlayer.fileFor(tour) != null -> {
                        go(Nav.sphere(nav, tour))
                        SphereStage.ensure()
                        SpherePlayer.play(tour)
                    }
                    Tour360.open(tour.url) -> go(Nav.handoff(nav))
                    else -> go(Nav.watch(nav, StreamSource(tour.url, tour.title, spherical = true, live = LiveState.NotLive)))
                }
            }
        }
    }

    private fun clampIndex() {
        val size = when (stage) {
            Stage.States -> states.size
            Stage.Live -> mandirsInState.size
            else -> return
        }
        if (size == 0) {
            if (stage == Stage.Live) go(Nav.backToStates(nav, 0))
            return
        }
        if (index >= size) nav = nav.copy(index = 0)
    }
}

object RingSlots {
    const val COUNT = 16
    val shown = mutableStateListOf<Int>().apply { repeat(COUNT) { add(-1) } }
}

/** Which cards get one of the sixteen panels. Pure, so the windowing can be tested. */
object RingWindow {
    /**
     * Cards that should be on panels when the member faces [facingDeg] relative to the ring's
     * anchor. With [count] ≤ [slots] every card is shown.
     */
    fun wanted(count: Int, slots: Int, facingDeg: Float): Set<Int> {
        if (count <= 0) return emptySet()
        if (count <= slots) return (0 until count).toSet()
        val spacing = 360f / count
        val nearest = kotlin.math.round(facingDeg / spacing).toInt()
        val half = slots / 2
        return (0 until slots).mapTo(LinkedHashSet()) { (nearest + it - half).floorMod(count) }
    }

    /**
     * Keeps cards that stay wanted on the panel they already have, so nothing flickers,
     * and hands freed panels to newly wanted cards. Returns true when any panel changed.
     */
    fun assign(shown: MutableList<Int>, wanted: Set<Int>): Boolean {
        var changed = false
        val placed = HashSet<Int>()
        val free = ArrayList<Int>(shown.size)
        for (slot in shown.indices) {
            val card = shown[slot]
            if (card in wanted && placed.add(card)) continue
            free.add(slot)
        }
        for (card in wanted) {
            if (card in placed) continue
            val slot = if (free.isEmpty()) break else free.removeAt(0)
            if (shown[slot] != card) {
                shown[slot] = card
                changed = true
            }
            placed.add(card)
        }
        for (slot in free) {
            if (shown[slot] != -1) {
                shown[slot] = -1
                changed = true
            }
        }
        return changed
    }
}
