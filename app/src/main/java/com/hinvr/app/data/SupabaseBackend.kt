package com.hinvr.app.data

import com.hinvr.app.BuildConfig
import com.hinvr.app.ui.catalog.Mandir
import com.hinvr.app.ui.catalog.ServiceTile
import com.hinvr.app.ui.catalog.parseTileScene
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String = "",
    val city: String = "",
    @SerialName("language_tag") val languageTag: String = "en",
    val audience: String = "Me",
    @SerialName("profile_complete") val profileComplete: Boolean = false,
    val tier: String = "None",
    @SerialName("member_id") val memberId: String = "",
    @SerialName("valid_until") val validUntil: String = "",
)

data class RemoteUser(
    val id: String,
    val phone: String,
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
    @SerialName("updated_label") val updatedLabel: String = "",
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

data class RemoteCatalog(
    val mandirs: List<Mandir>,
    val services: List<ServiceTile>,
    val headline: String?,
)

/**
 * Supabase client for phone OTP, session, [profiles], and the public catalog.
 *
 * Phone SMS stays mocked while [BuildConfig.MOCK_PHONE_OTP] is true
 * (default). Catalog still loads whenever URL + publishable key are set.
 */
class SupabaseBackend {

    val mockPhoneOtp: Boolean = BuildConfig.MOCK_PHONE_OTP

    val hasCloud: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    val configured: Boolean = !mockPhoneOtp && hasCloud

    private val client: SupabaseClient? = if (hasCloud) {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Auth)
            install(Postgrest)
        }
    } else {
        null
    }

    suspend fun sendPhoneOtp(phone: String): String = io {
        if (mockPhoneOtp || !configured) return@io LOCAL_USER
        try {
            client!!.auth.signInWith(OTP) { this.phone = phone }
            phone
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
    }

    suspend fun verifyOtp(phone: String, token: String) = io {
        if (mockPhoneOtp || !configured) return@io
        try {
            client!!.auth.verifyPhoneOtp(
                type = OtpType.Phone.SMS,
                phone = phone,
                token = token,
            )
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
    }

    suspend fun currentUser(): RemoteUser? = io {
        if (!configured) return@io null
        val sb = client ?: return@io null
        val user = try {
            sb.auth.currentUserOrNull()
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        } ?: return@io null
        val profile = runCatching {
            sb.from("profiles").select {
                filter { eq("id", user.id) }
            }.decodeSingleOrNull<ProfileRow>()
        }.getOrNull()
        RemoteUser(
            id = user.id,
            phone = user.phone.orEmpty(),
            displayName = profile?.displayName.orEmpty(),
            profile = profile,
        )
    }

    suspend fun saveProfile(
        name: String,
        city: String,
        languageTag: String,
        audience: Audience,
    ) = io {
        if (!configured) return@io
        val sb = client ?: return@io
        val userId = sb.auth.currentUserOrNull()?.id
            ?: throw AuthException("Sign in again from your number.")
        val existing = runCatching {
            sb.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<ProfileRow>()
        }.getOrNull()
        try {
            sb.from("profiles").upsert(
                ProfileRow(
                    id = userId,
                    displayName = name,
                    city = city,
                    languageTag = languageTag,
                    audience = audience.name,
                    profileComplete = true,
                    tier = existing?.tier ?: MembershipTier.None.name,
                    memberId = existing?.memberId.orEmpty(),
                    validUntil = existing?.validUntil.orEmpty(),
                ),
            )
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
    }

    suspend fun saveTier(
        tier: MembershipTier,
        memberId: String,
        validUntilLabel: String,
    ) = io {
        if (!configured) return@io
        val sb = client ?: return@io
        val userId = sb.auth.currentUserOrNull()?.id
            ?: throw AuthException("Sign in again from your number.")
        val existing = runCatching {
            sb.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<ProfileRow>()
        }.getOrNull()
        try {
            sb.from("profiles").upsert(
                ProfileRow(
                    id = userId,
                    displayName = existing?.displayName.orEmpty(),
                    city = existing?.city.orEmpty(),
                    languageTag = existing?.languageTag ?: "en",
                    audience = existing?.audience ?: Audience.Me.name,
                    profileComplete = existing?.profileComplete == true,
                    tier = tier.name,
                    memberId = memberId,
                    validUntil = validUntilLabel,
                ),
            )
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
    }

    suspend fun signOut() = io {
        if (!configured) return@io
        runCatching { client!!.auth.signOut() }
    }

    suspend fun fetchCatalog(): RemoteCatalog? = io {
        if (!hasCloud) return@io null
        val sb = client ?: return@io null
        try {
            val mandirs = sb.from("mandirs").select {
                filter { eq("published", true) }
                order("sort_order", Order.ASCENDING)
            }.decodeList<MandirRow>().map { row ->
                Mandir(
                    id = row.id,
                    name = row.name,
                    place = row.place,
                    city = row.city,
                    scene = parseTileScene(row.scene, row.id),
                    live = row.live,
                    vr = row.vr,
                    passAccepted = row.passAccepted,
                    nextAarti = row.nextAarti,
                    updatedLabel = row.updatedLabel.ifBlank { "Updated just now" },
                    timings = row.timings,
                    photoUrl = row.photoUrl,
                    liveUrl = row.liveUrl,
                    vrUrl = row.vrUrl,
                    deity = row.deity,
                    summary = row.summary,
                    history = row.history,
                    significance = row.significance,
                    architecture = row.architecture,
                    dressCode = row.dressCode,
                    bestTime = row.bestTime,
                    visitorNotes = row.visitorNotes,
                    facilities = row.facilities,
                    address = row.address,
                    officialWebsite = row.officialWebsite,
                    contactPhone = row.contactPhone,
                )
            }
            val services = sb.from("home_services").select {
                filter { eq("published", true) }
                order("sort_order", Order.ASCENDING)
            }.decodeList<ServiceRow>().map { row ->
                ServiceTile(
                    title = row.title,
                    benefit = row.benefit,
                    scene = parseTileScene(row.scene, row.id),
                    route = row.route,
                    tall = row.tall,
                    photoUrl = row.photoUrl,
                )
            }
            val headline = runCatching {
                sb.from("app_settings").select {
                    filter { eq("key", "home") }
                }.decodeSingleOrNull<SettingRow>()?.value?.headline?.ifBlank { null }
            }.getOrNull()
            RemoteCatalog(mandirs, services, headline)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val LOCAL_USER = "local"
    }
}

class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

private fun humanize(e: Throwable): String {
    val rest = e as? RestException
    val message = rest?.message ?: e.message.orEmpty()
    val status = rest?.statusCode ?: 0
    return when {
        status == 429 || message.contains("rate", ignoreCase = true) ->
            "Too many tries. Wait a minute, then send again."
        status == 401 || message.contains("otp", ignoreCase = true) &&
            message.contains("invalid", ignoreCase = true) ->
            "That code didn’t match. Try again."
        message.contains("phone", ignoreCase = true) && message.contains("invalid", ignoreCase = true) ->
            "That number doesn’t look right."
        message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("failed to connect", ignoreCase = true) ||
            message.contains("Unable to connect", ignoreCase = true) ->
            "No network. Try again."
        message.isNotBlank() -> message
        else -> "Couldn’t reach HINVR. Try again."
    }
}

private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
