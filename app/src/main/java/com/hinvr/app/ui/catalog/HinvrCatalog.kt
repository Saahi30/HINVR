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
    val deity: String = "",
    val summary: String = "",
    val history: String = "",
    val significance: String = "",
    val architecture: String = "",
    val dressCode: String = "",
    val bestTime: String = "",
    val visitorNotes: String = "",
    val facilities: String = "",
    val address: String = "",
    val officialWebsite: String = "",
    val contactPhone: String = "",
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
            liveUrl = "https://www.youtube.com/embed/live_stream?channel=UCTboTRX74UydvU_cBdm_cCQ",
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
            liveUrl = "https://www.youtube.com/embed/6Uf3aMujHa8",
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
        Mandir(
            id = "vaishno-devi",
            name = "Vaishno Devi",
            place = "Katra, Jammu and Kashmir",
            city = "Katra",
            scene = TileScene.Kedarnath,
            passAccepted = true,
            nextAarti = "6:20 PM",
            timings = "Open throughout the day · Aarti morning and evening",
        ),
        Mandir(
            id = "meenakshi",
            name = "Meenakshi Amman",
            place = "Madurai, Tamil Nadu",
            city = "Madurai",
            scene = TileScene.Tirupati,
            passAccepted = true,
            nextAarti = "7:30 PM",
            timings = "Morning 5:00 AM–12:30 PM · Evening 4:00–10:00 PM",
        ),
        Mandir(
            id = "jagannath",
            name = "Jagannath Temple",
            place = "Puri, Odisha",
            city = "Puri",
            scene = TileScene.Somnath,
            nextAarti = "6:00 PM",
            timings = "Open 5:30 AM–9:30 PM · Timings vary by ritual",
        ),
        Mandir(
            id = "dwarkadhish",
            name = "Dwarkadhish",
            place = "Dwarka, Gujarat",
            city = "Dwarka",
            scene = TileScene.Somnath,
            passAccepted = true,
            nextAarti = "7:30 PM",
            timings = "Morning 6:30 AM–1:00 PM · Evening 5:00–9:30 PM",
        ),
        Mandir(
            id = "badrinath",
            name = "Badrinath",
            place = "Chamoli, Uttarakhand",
            city = "Badrinath",
            scene = TileScene.Kedarnath,
            vr = true,
            nextAarti = "6:00 PM",
            timings = "Seasonal opening · Morning and evening darshan",
        ),
        Mandir(
            id = "golden-temple",
            name = "Sri Harmandir Sahib",
            place = "Amritsar, Punjab",
            city = "Amritsar",
            scene = TileScene.Kashi,
            nextAarti = "6:00 PM",
            timings = "Open daily · Palki Sahib ceremonies morning and night",
        ),
        Mandir(
            id = "siddhivinayak",
            name = "Siddhivinayak",
            place = "Mumbai, Maharashtra",
            city = "Mumbai",
            scene = TileScene.Shirdi,
            passAccepted = true,
            nextAarti = "7:30 PM",
            timings = "Wednesday–Monday 5:30 AM–9:50 PM · Tuesday from 3:15 AM",
        ),
        Mandir(
            id = "iskcon-bengaluru",
            name = "ISKCON Bengaluru",
            place = "Rajajinagar, Karnataka",
            city = "Bengaluru",
            scene = TileScene.Tirupati,
            passAccepted = true,
            nextAarti = "7:00 PM",
            timings = "Morning 4:15 AM–5:00 AM · Darshan through the evening",
        ),
    ).map(::withBundledInformation)

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

private fun withBundledInformation(mandir: Mandir): Mandir = when (mandir.id) {
    "tirupati" -> mandir.copy(
        deity = "Sri Venkateswara",
        summary = "A hill sanctuary at Tirumala and one of India’s most visited places of worship.",
        history = "The living shrine grew through the patronage of South Indian dynasties and Vijayanagara rulers. Its ritual tradition is older than many structures visible today.",
        significance = "Sri Venkateswara is worshipped as Vishnu offering refuge in the present age. Darshan, laddu prasadam, and vows are central to the pilgrimage.",
        architecture = "Dravidian architecture with a gilded Ananda Nilayam vimana, gopurams, pillared halls, and layered prakaram corridors.",
        dressCode = "Traditional or modest clothing. Temple dress rules are stricter for booked sevas.",
        bestTime = "Weekday mornings outside major festivals. Expect substantial crowds year-round.",
        visitorNotes = "Use official TTD channels. Phones, cameras, footwear, and luggage are restricted inside.",
        facilities = "Official booking\nFree Tirumala buses\nLuggage counters\nWheelchair assistance\nPrasadam counters",
        address = "Tirumala, Tirupati, Andhra Pradesh 517504",
        officialWebsite = "https://www.tirumala.org/",
        contactPhone = "1800 425 4141",
    )
    "kashi" -> mandir.copy(
        deity = "Shri Vishwanath",
        summary = "A jyotirlinga shrine in the sacred lanes of Kashi, beside the Ganga.",
        history = "The present shrine was built under Ahilyabai Holkar in the eighteenth century, with later gold-plated additions and the contemporary Dham corridor.",
        significance = "Kashi is revered as Shiva’s city. Pilgrims combine Vishwanath darshan with Ganga snan, Annapurna darshan, and ancestral rites.",
        architecture = "Gold-clad North Indian shikharas, stone courtyards, and a corridor linking the old lanes toward the ghats.",
        dressCode = "Modest clothing; leave leather goods and restricted electronics in official lockers.",
        bestTime = "Early weekday mornings, or Mangala Aarti with an official booking.",
        visitorNotes = "Carry accepted photo ID and follow current security-gate instructions.",
        facilities = "Official lockers\nSelected wheelchair routes\nDrinking water\nPrasad counters\nGanga corridor",
        address = "Lahori Tola, Varanasi, Uttar Pradesh 221001",
        officialWebsite = "https://shrikashivishwanath.org/",
    )
    "shirdi" -> mandir.copy(
        deity = "Shri Sai Baba",
        summary = "The samadhi shrine of Sai Baba, centred on faith, patience, and service.",
        history = "The Samadhi Mandir developed around the site where Sai Baba’s mortal remains were interred in 1918.",
        significance = "Kakad, Madhyan, Dhup, and Shej aartis structure a devotional day open to people across traditions.",
        architecture = "A marble samadhi hall connected to Dwarkamai, Chavadi, and places associated with Sai Baba’s life.",
        dressCode = "Clean, modest clothing with shoulders and knees covered.",
        bestTime = "Weekday mornings outside Thursdays, holidays, and festivals.",
        visitorNotes = "Use the Sansthan’s official portal; ignore unofficial paid live-darshan claims.",
        facilities = "Official darshan booking\nPrasadalaya\nAccommodation desk\nLuggage counters\nWheelchair assistance",
        address = "Mauli Nagar, Shirdi, Maharashtra 423109",
        officialWebsite = "https://sai.org.in/",
    )
    "kedarnath" -> mandir.copy(
        deity = "Lord Shiva",
        summary = "A Himalayan jyotirlinga beneath the Kedarnath massif, reached through a demanding seasonal pilgrimage.",
        history = "Tradition links the shrine to the Pandavas and its revival to Adi Shankaracharya. Its massive stone form has endured a severe climate.",
        significance = "One of the twelve jyotirlingas and a principal Uttarakhand Char Dham stop.",
        architecture = "Massive grey stone blocks, a pyramidal tower, and a compact mandapa adapted to the high Himalaya.",
        dressCode = "Warm layers, rain protection, sturdy footwear, and modest temple attire.",
        bestTime = "Late May–June or September, subject to weather and official seasonal dates.",
        visitorNotes = "Check medical fitness, registration, weather, trek, and helicopter advisories.",
        facilities = "Seasonal registration\nMedical posts\nHelicopter services\nPony and palanquin\nLimited accessibility",
        address = "Kedarnath, Rudraprayag, Uttarakhand 246445",
        officialWebsite = "https://badrinath-kedarnath.gov.in/",
    )
    "somnath" -> mandir.copy(
        deity = "Lord Shiva",
        summary = "A sea-facing jyotirlinga at Prabhas Patan, rebuilt as a symbol of continuity.",
        history = "Somnath is remembered through repeated rebuilding. The present twentieth-century temple follows the Maru-Gurjara tradition.",
        significance = "Revered as the first among the twelve jyotirlingas and associated with the sacred Prabhas confluence.",
        architecture = "A sandstone shikhara, carved mandapas, axial sea view, and the south-pointing arrow pillar.",
        dressCode = "Modest clothing; follow current electronics and baggage restrictions.",
        bestTime = "October–March, arriving before sunset and evening aarti.",
        visitorNotes = "Check official timings for the evening programme and current photography rules.",
        facilities = "Prasad counters\nWheelchair access\nCloakroom\nParking\nEvening programme",
        address = "Somnath Mandir Road, Prabhas Patan, Gujarat 362268",
        officialWebsite = "https://somnath.org/",
    )
    else -> mandir.copy(
        deity = when (mandir.id) {
            "vaishno-devi" -> "Mata Vaishno Devi"
            "meenakshi" -> "Meenakshi and Sundareshwar"
            "jagannath" -> "Lord Jagannath"
            "dwarkadhish" -> "Lord Krishna"
            "badrinath" -> "Lord Badri Narayan"
            "golden-temple" -> "Sri Guru Granth Sahib"
            "siddhivinayak" -> "Lord Ganesha"
            "iskcon-bengaluru" -> "Radha Krishnachandra"
            else -> ""
        },
        summary = "${mandir.name} is a living place of worship in ${mandir.place}, with timings and visitor guidance kept current by the HINVR desk.",
        dressCode = "Wear clean, modest clothing and follow the notices at the place of worship.",
        bestTime = mandir.nextAarti?.let { "Arrive before the next scheduled aarti at $it." }
            ?: "Weekday mornings are usually calmer.",
        visitorNotes = "Confirm official timings, closures, and entry rules before travelling.",
        address = mandir.place,
    )
}
