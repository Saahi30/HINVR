package com.hinvr.app.ui.catalog

import com.hinvr.app.navigation.Destinations

enum class TileScene {
    LiveAarti,
    VrHall,
    PassDesk,
    PanditDoor,
    ConciergeDesk,
    YatraRoad,
    Tirupati,
    Kashi,
    Shirdi,
    Kedarnath,
    Somnath,
}

fun parseTileScene(raw: String, fallbackId: String = ""): TileScene {
    val key = raw.ifBlank { fallbackId }
    return TileScene.entries.find { it.name.equals(key, true) } ?: TileScene.Tirupati
}

data class ServiceTile(
    val title: String,
    val benefit: String,
    val scene: TileScene,
    val route: String,
    val tall: Boolean = false,
    val photoUrl: String = "",
)

data class Mandir(
    val id: String,
    val name: String,
    val place: String,
    val city: String,
    val scene: TileScene,
    val live: Boolean = false,
    val vr: Boolean = false,
    val passAccepted: Boolean = false,
    val nextAarti: String? = null,
    val updatedLabel: String = "Updated 12 min ago",
    val timings: String = "Suprabhatam 3:30 AM · Aarti through the evening",
    val photoUrl: String = "",
    val liveUrl: String = "",
    val vrUrl: String = "",
)

data class ChatBlock(
    val id: String,
    val time: String,
    val photoTitle: String? = null,
    val photoPlace: String? = null,
    val photoScene: TileScene? = null,
    val factTitle: String? = null,
    val factBody: String? = null,
)

object HinvrCatalog {
    const val DefaultHeadline = "You see the aarti.\nThe internet sees a thumbnail."

    val bundledServices = listOf(
        ServiceTile("Live Darshan", "Aarti, from the sabha", TileScene.LiveAarti, Destinations.Live, tall = true),
        ServiceTile("VR Darshan", "360. Move your phone", TileScene.VrHall, Destinations.Vr),
        ServiceTile("Priority Pass", "Show this at the desk", TileScene.PassDesk, Destinations.Pass),
        ServiceTile("Book Pandit", "One city. Verified", TileScene.PanditDoor, Destinations.Pooja),
        ServiceTile("Concierge", "Ask. Then a human", TileScene.ConciergeDesk, Destinations.Concierge),
        ServiceTile("Yatra", "Club desk, not a portal", TileScene.YatraRoad, Destinations.Yatra, tall = true),
    )

    val bundledMandirs = listOf(
        Mandir(
            id = "tirupati",
            name = "Sri Venkateswara",
            place = "Tirupati, Andhra Pradesh",
            city = "Tirupati",
            scene = TileScene.Tirupati,
            live = true,
            vr = true,
            passAccepted = true,
            updatedLabel = "Updated 12 min ago",
            timings = "Suprabhatam 2:30 AM · Tomala 3:30 AM · Ekantha 1:00 AM",
        ),
        Mandir(
            id = "kashi",
            name = "Kashi Vishwanath",
            place = "Varanasi, Uttar Pradesh",
            city = "Kashi",
            scene = TileScene.Kashi,
            live = false,
            vr = true,
            passAccepted = true,
            nextAarti = "4:30 PM",
            timings = "Mangala 3:00 AM · Saptarishi aarti 7:00 PM",
        ),
        Mandir(
            id = "shirdi",
            name = "Sai Baba",
            place = "Shirdi, Maharashtra",
            city = "Shirdi",
            scene = TileScene.Shirdi,
            live = true,
            passAccepted = true,
            updatedLabel = "Updated 8 min ago",
            timings = "Kakad aarti 5:00 AM · Shej aarti 10:00 PM",
        ),
        Mandir(
            id = "kedarnath",
            name = "Kedarnath",
            place = "Rudraprayag, Uttarakhand",
            city = "Kedarnath",
            scene = TileScene.Kedarnath,
            vr = true,
            timings = "Opening aarti 4:00 AM · Closed in winter",
        ),
        Mandir(
            id = "somnath",
            name = "Somnath",
            place = "Gir Somnath, Gujarat",
            city = "Somnath",
            scene = TileScene.Somnath,
            passAccepted = true,
            timings = "Mangala 6:00 AM · Aarti 7:00 PM",
        ),
    )

    val services: List<ServiceTile> get() = bundledServices
    val mandirs: List<Mandir> get() = bundledMandirs
    val liveNow: Mandir get() = mandirs.firstOrNull { it.live } ?: mandirs.first()

    fun mandir(id: String): Mandir =
        mandirs.find { it.id.equals(id, true) || it.city.equals(id, true) }
            ?: mandirs.first().copy(id = id, name = id.replaceFirstChar { it.uppercase() }, city = id)

    fun mergeServices(remote: List<ServiceTile>): List<ServiceTile> {
        if (remote.isEmpty()) return bundledServices
        val byRoute = remote.associateBy { it.route }
        return bundledServices.map { local ->
            byRoute[local.route]?.copy(tall = local.tall) ?: local
        }
    }

    val conciergeThread = listOf(
        ChatBlock(
            id = "1",
            time = "Yesterday",
            factTitle = "Specialists 8am–10pm IST",
            factBody = "Family custom may differ. We are not a replacement for your kulguru.",
        ),
        ChatBlock(
            id = "2",
            time = "09:14",
            photoTitle = "Tirupati with elderly parents",
            photoPlace = "Desk note",
            photoScene = TileScene.Tirupati,
        ),
        ChatBlock(
            id = "3",
            time = "09:16",
            factTitle = "What to wear at Kashi Vishwanath",
            factBody = "Modest clothing, covered shoulders. Leather is often left outside. Follow the notice at the mandir.",
        ),
    )

    val faqs = listOf(
        "wear-kashi" to "What to wear at Kashi Vishwanath",
        "shraddh" to "Shraddh — first 13 days",
        "tirupati-elderly" to "Best time for Tirupati with elderly parents",
    )
}
