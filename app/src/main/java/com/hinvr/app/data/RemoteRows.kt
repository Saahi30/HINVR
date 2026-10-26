package com.hinvr.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A profiles row as read. Desk-owned columns are nullable: valid_until is a SQL
 * date (or null), and older rows may still hold a free-text label.
 */
@Serializable
data class ProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String = "",
    val city: String = "",
    @SerialName("language_tag") val languageTag: String = "en",
    val audience: String = "Me",
    @SerialName("profile_complete") val profileComplete: Boolean = false,
    val tier: String? = null,
    @SerialName("member_id") val memberId: String? = null,
    @SerialName("valid_until") val validUntil: String? = null,
    @SerialName("phone_e164") val phoneE164: String = "",
    val addresses: List<MemberPlace> = emptyList(),
)

/**
 * The columns a member may write. Tier, member ID and validity belong to the desk
 * and are never sent, so a stale read can't overwrite them. No defaults, so every
 * field is encoded even when it equals the column default.
 */
@Serializable
internal data class ProfileUpsert(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val city: String,
    @SerialName("language_tag") val languageTag: String,
    val audience: String,
    @SerialName("profile_complete") val profileComplete: Boolean,
    @SerialName("phone_e164") val phoneE164: String,
    val addresses: List<MemberPlace>,
)

/** Members insert kind, summary, city and mandir. Status and staff notes are the desk's. */
@Serializable
internal data class DeskRequestInsert(
    @SerialName("user_id") val userId: String,
    val kind: String,
    val summary: String,
    val city: String,
    @SerialName("mandir_id") val mandirId: String? = null,
)

@Serializable
internal data class MembershipRequestInsert(
    @SerialName("user_id") val userId: String,
    val tier: String,
    @SerialName("amount_inr") val amountInr: Int,
)

@Serializable
internal data class MembershipRequestPatch(
    val tier: String,
    @SerialName("amount_inr") val amountInr: Int,
)

@Serializable
data class MembershipRequestRow(
    val tier: String = "",
    val status: String = "",
    @SerialName("staff_note") val staffNote: String = "",
)

/** The server fills member_id from the profile; the app does not send it. */
@Serializable
internal data class PhysicalCardInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("ship_name") val shipName: String,
    @SerialName("ship_address") val shipAddress: String,
)

@Serializable
internal data class PhysicalCardRow(
    val status: String = "",
)

@Serializable
internal data class PassCheckInRow(
    val id: String = "",
    val place: String = "",
    val note: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

data class MemberDeskState(
    val request: MembershipRequestRow?,
    val checkIns: List<PassCheckIn>,
    val cardStatus: String? = null,
)

data class RemoteUser(
    val id: String,
    val phone: String,
    val email: String,
    val displayName: String,
    val profile: ProfileRow?,
)

@Serializable
data class MandirRow(
    val id: String,
    val name: String,
    val place: String = "",
    val city: String = "",
    val scene: String = "",
    @SerialName("photo_url") val photoUrl: String = "",
    val live: Boolean = false,
    val vr: Boolean = false,
    @SerialName("pass_accepted") val passAccepted: Boolean = false,
    @SerialName("next_aarti") val nextAarti: String? = null,
    val timings: String = "",
    @SerialName("live_checked_at") val liveCheckedAt: String? = null,
    @SerialName("live_url") val liveUrl: String = "",
    @SerialName("vr_url") val vrUrl: String = "",
    val deity: String = "",
    val summary: String = "",
    val history: String = "",
    val significance: String = "",
    val architecture: String = "",
    @SerialName("dress_code") val dressCode: String = "",
    @SerialName("best_time") val bestTime: String = "",
    @SerialName("visitor_notes") val visitorNotes: String = "",
    val facilities: String = "",
    val address: String = "",
    @SerialName("official_website") val officialWebsite: String = "",
    @SerialName("contact_phone") val contactPhone: String = "",
)

@Serializable
data class ServiceRow(
    val id: String,
    val title: String,
    val benefit: String,
    @SerialName("photo_url") val photoUrl: String = "",
    val route: String,
    val scene: String = "",
    val tall: Boolean = false,
)

@Serializable
data class HomeSettingsValue(
    val headline: String = "",
    val eyebrow: String = "",
)

@Serializable
data class SettingRow(
    val key: String,
    val value: HomeSettingsValue = HomeSettingsValue(),
)

@Serializable
data class NoticeRow(
    val id: String,
    val title: String = "",
    val body: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
internal data class DeviceTokenArg(
    @SerialName("p_token") val token: String,
)

@Serializable
data class VrDeviceRow(
    @SerialName("device_id") val deviceId: String,
    val name: String = "",
    @SerialName("paired_at") val pairedAt: String = "",
    @SerialName("last_seen_at") val lastSeenAt: String = "",
)

@Serializable
data class VrPairingCode(
    val code: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
internal data class VrDeviceArg(
    @SerialName("p_device_id") val deviceId: String,
)

/** The published catalog exactly as the desk ordered it. Cached so offline launches keep it. */
@Serializable
data class RemoteCatalog(
    val mandirs: List<MandirRow>,
    val services: List<ServiceRow>,
    val headline: String? = null,
)
