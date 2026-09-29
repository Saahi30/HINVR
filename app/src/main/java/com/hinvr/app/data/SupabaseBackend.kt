package com.hinvr.app.data

import com.hinvr.app.BuildConfig
import com.hinvr.app.ui.catalog.Mandir
import com.hinvr.app.ui.catalog.ServiceTile
import com.hinvr.app.ui.catalog.parseTileScene
import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.OtpVerifyResult
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.serializer.KotlinXSerializer
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject

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
    @SerialName("phone_e164") val phoneE164: String = "",
    val addresses: List<MemberPlace> = emptyList(),
)

@Serializable
private data class PhonePatch(
    @SerialName("phone_e164") val phoneE164: String,
)

@Serializable
private data class DeskRequestInsert(
    @SerialName("user_id") val userId: String,
    val kind: String,
    val summary: String,
    val city: String = "",
    @SerialName("mandir_id") val mandirId: String? = null,
)

@Serializable
private data class MembershipRequestInsert(
    @SerialName("user_id") val userId: String,
    val tier: String,
    @SerialName("amount_inr") val amountInr: Int,
)

@Serializable
private data class MembershipRequestPatch(
    val tier: String,
    @SerialName("amount_inr") val amountInr: Int,
)

@Serializable
data class MembershipRequestRow(
    val tier: String = "",
    val status: String = "",
    @SerialName("staff_note") val staffNote: String = "",
)

@Serializable
private data class PhysicalCardInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("ship_name") val shipName: String,
    @SerialName("ship_address") val shipAddress: String,
)

@Serializable
private data class PhysicalCardRow(
    val status: String = "",
)

@Serializable
private data class PassCheckInRow(
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

@Serializable
data class NoticeRow(
    val id: String,
    val title: String = "",
    val body: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
private data class DeviceTokenArg(
    @SerialName("p_token") val token: String,
)

data class RemoteCatalog(
    val mandirs: List<Mandir>,
    val services: List<ServiceTile>,
    val headline: String?,
)

/**
 * Supabase client for email accounts, session, [profiles], and the public catalog.
 *
 * Accounts are live whenever URL + publishable key are set. Phone SMS is not
 * used: this project has the phone provider turned off.
 */
class SupabaseBackend {

    val hasCloud: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    val configured: Boolean = hasCloud

    private val client: SupabaseClient? = if (hasCloud) {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Auth) {
                // Recovery emails return here. Password sign-in does not use this redirect.
                flowType = FlowType.PKCE
                scheme = PASSWORD_RESET_SCHEME
                host = PASSWORD_RESET_HOST
            }
            install(Postgrest) {
                serializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
            }
        }
    } else {
        null
    }

    suspend fun signUp(email: String, password: String) = io {
        val sb = requireClient()
        try {
            sb.auth.signUpWith(Email, redirectUrl = null) {
                this.email = email
                this.password = password
            }
            if (sb.auth.currentUserOrNull() == null) {
                throw AuthException("Check your email to finish creating the account.")
            }
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
    }

    suspend fun signIn(email: String, password: String) = io {
        val sb = requireClient()
        try {
            sb.auth.signInWith(Email, redirectUrl = null) {
                this.email = email
                this.password = password
            }
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
    }

    /**
     * Emails a recovery link. Prefers the app redirect so the link opens the
     * reset screen. If that URL is not allowed yet, falls back to the project
     * site URL and the member pastes the link.
     *
     * @return true when the email link can open this app.
     */
    suspend fun sendPasswordReset(email: String): Boolean = io {
        val sb = requireClient()
        try {
            sb.auth.resetPasswordForEmail(email, redirectUrl = PASSWORD_RESET_URL)
            true
        } catch (e: Exception) {
            if (!redirectRejected(e)) throw AuthException(humanize(e), e)
            try {
                sb.auth.resetPasswordForEmail(email, redirectUrl = null)
                false
            } catch (again: Exception) {
                throw AuthException(humanize(again), again)
            }
        }
    }

    /** Turns a recovery email link, or the app redirect, into a session. */
    suspend fun acceptRecoveryLink(link: String) = io {
        val sb = requireClient()
        val trimmed = link.trim()
        try {
            val code = queryParam(trimmed, "code")
            val type = queryParam(trimmed, "type")
            val opensApp = trimmed.startsWith("$PASSWORD_RESET_URL", ignoreCase = true)
            if (!code.isNullOrBlank() && (opensApp || type.equals("recovery", ignoreCase = true))) {
                sb.auth.exchangeCodeForSession(code)
                return@io
            }
            val fragment = trimmed.substringAfter('#', "")
            val accessToken = queryParam(fragment, "access_token")
            val fragmentType = queryParam(fragment, "type")
            if (!accessToken.isNullOrBlank() && fragmentType.equals("recovery", ignoreCase = true)) {
                val refresh = queryParam(fragment, "refresh_token").orEmpty()
                sb.auth.importAuthToken(accessToken, refresh, retrieveUser = true)
                return@io
            }
            val token = queryParam(trimmed, "token") ?: queryParam(trimmed, "token_hash")
            if (token.isNullOrBlank() || !type.equals("recovery", ignoreCase = true)) {
                throw AuthException("That isn’t a password reset link.")
            }
            when (sb.auth.verifyEmailOtp(OtpType.Email.RECOVERY, tokenHash = token)) {
                is OtpVerifyResult.Authenticated -> Unit
                OtpVerifyResult.VerifiedNoSession ->
                    throw AuthException("That link didn’t sign you in. Request a new one.")
            }
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
    }

    suspend fun updatePassword(newPassword: String) = io {
        val sb = requireClient()
        try {
            sb.auth.updateUser {
                password = newPassword
            }
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
    }

    suspend fun hasSession(): Boolean = io {
        val sb = client ?: return@io false
        sb.auth.awaitInitialization()
        sb.auth.currentSessionOrNull() != null
    }

    fun handleAuthCallback(intent: Intent, onRecovered: () -> Unit, onFailure: (String) -> Unit) {
        val data = intent.data ?: return
        if (data.scheme != PASSWORD_RESET_SCHEME || data.host != PASSWORD_RESET_HOST) return
        val sb = client ?: return
        if (data.getQueryParameter("code").isNullOrBlank()) {
            onFailure("That reset link didn’t include a code. Paste the link from the email instead.")
            return
        }
        sb.handleDeeplinks(
            intent,
            onSessionSuccess = { onRecovered() },
            onError = { onFailure(humanize(it)) },
        )
    }

    suspend fun currentUser(): RemoteUser? = io {
        if (!configured) return@io null
        val sb = client ?: return@io null
        val user = try {
            sb.auth.awaitInitialization()
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
            phone = profile?.phoneE164?.ifBlank { user.phone.orEmpty() }.orEmpty(),
            email = user.email.orEmpty(),
            displayName = profile?.displayName.orEmpty(),
            profile = profile,
        )
    }

    suspend fun saveProfile(
        name: String,
        city: String,
        languageTag: String,
        audience: Audience,
        phoneE164: String,
        addresses: List<MemberPlace>,
    ) = io {
        val sb = requireClient()
        val userId = sb.auth.currentUserOrNull()?.id
            ?: throw AuthException("Sign in again.")
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
                    phoneE164 = phoneE164.ifBlank { existing?.phoneE164.orEmpty() },
                    addresses = addresses.ifEmpty { existing?.addresses.orEmpty() },
                ),
            )
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
    }

    suspend fun requestMembership(tier: MembershipTier, amountInr: Int) = io {
        val sb = requireClient()
        val userId = sb.auth.currentUserOrNull()?.id
            ?: throw AuthException("Sign in again.")
        val pending = try {
            sb.from("membership_requests").select {
                filter {
                    eq("user_id", userId)
                    eq("status", "pending")
                }
                limit(1)
            }.decodeList<MembershipRequestRow>()
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
        try {
            if (pending.isNotEmpty()) {
                sb.from("membership_requests").update(
                    MembershipRequestPatch(tier = tier.name, amountInr = amountInr),
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("status", "pending")
                    }
                }
            } else {
                sb.from("membership_requests").insert(
                    MembershipRequestInsert(
                        userId = userId,
                        tier = tier.name,
                        amountInr = amountInr,
                    ),
                )
            }
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
    }

    suspend fun fetchMemberDesk(userId: String): MemberDeskState? = io {
        val sb = client ?: return@io null
        if (!hasCloud || userId.isBlank()) return@io null
        try {
            val request = sb.from("membership_requests").select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(1)
            }.decodeList<MembershipRequestRow>().firstOrNull()
            val checkIns = sb.from("pass_checkins").select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(30)
            }.decodeList<PassCheckInRow>().map { row ->
                PassCheckIn(
                    id = row.id,
                    place = row.place,
                    note = row.note,
                    createdAt = row.createdAt,
                )
            }
            val cardStatus = try {
                val latest = sb.from("physical_card_requests").select(columns = Columns.list("status")) {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                    limit(1)
                }.decodeList<PhysicalCardRow>().firstOrNull()
                if (latest == null || latest.status == "cancelled") "" else latest.status
            } catch (_: Exception) {
                null
            }
            MemberDeskState(request = request, checkIns = checkIns, cardStatus = cardStatus)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun savePhone(phoneE164: String) = io {
        if (!hasCloud || phoneE164.isBlank()) return@io
        val sb = client ?: return@io
        val userId = sb.auth.currentUserOrNull()?.id ?: return@io
        runCatching {
            sb.from("profiles").update(PhonePatch(phoneE164)) {
                filter { eq("id", userId) }
            }
        }
    }

    /** Best-effort. Keeps the local desk card even if Cloud is off or the insert fails. */
    suspend fun createDeskRequest(
        kind: String,
        summary: String,
        city: String,
        mandirId: String? = null,
    ): Boolean = io {
        if (!hasCloud) return@io false
        val sb = client ?: return@io false
        val userId = try {
            sb.auth.currentUserOrNull()?.id
        } catch (_: Exception) {
            null
        } ?: return@io false
        try {
            sb.from("desk_requests").insert(
                DeskRequestInsert(
                    userId = userId,
                    kind = kind,
                    summary = summary,
                    city = city,
                    mandirId = mandirId?.ifBlank { null },
                ),
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    /** One-time browser URL that signs this member into the membership site. */
    suspend fun membershipPageUrl(): String = io {
        val sb = requireClient()
        sb.auth.awaitInitialization()
        val access = sb.auth.currentAccessTokenOrNull()
            ?: throw AuthException("Sign in again.")
        val endpoint = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/membership-handoff"
        val connection = (URI(endpoint).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $access")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        connection.outputStream.use { it.write("{}".toByteArray()) }
        val code = connection.responseCode
        val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.readText()
            .orEmpty()
        val body = runCatching { JSONObject(text) }.getOrNull()
        val token = body?.optString("token_hash").orEmpty()
        if (code !in 200..299 || token.isBlank()) {
            throw AuthException(body?.optString("error").orEmpty().ifBlank {
                if (code == 401) "Sign in again." else "Couldn't open membership."
            })
        }
        val type = body?.optString("type").orEmpty().ifBlank { "magiclink" }
        val base = BuildConfig.MEMBERSHIP_URL.trim()
        val joiner = if ('?' in base) "&" else "?"
        base + joiner +
            "handoff=" + URLEncoder.encode(token, Charsets.UTF_8.name()) +
            "&type=" + URLEncoder.encode(type, Charsets.UTF_8.name())
    }

    data class IssuedPass(val token: String, val expiresAt: Long)

    suspend fun issuePass(rotate: Boolean): IssuedPass = io {
        val body = JSONObject().put("rotate", rotate).put("purpose", "desk")
        val parsed = postFunction("pass-credential", body)
        val token = parsed.optString("token")
        val expires = parsed.optString("expires_at")
        if (token.isBlank() || !token.startsWith("HNV1.") || expires.isBlank()) {
            throw AuthException("Couldn't refresh the pass.")
        }
        IssuedPass(token, java.time.Instant.parse(expires).toEpochMilli())
    }

    suspend fun googleWalletUrl(): String = io {
        val parsed = postFunction("pass-wallet", JSONObject())
        val url = parsed.optString("url")
        if (!url.startsWith("https://pay.google.com/")) {
            throw AuthException(parsed.optString("error").ifBlank { "Couldn't open Google Wallet." })
        }
        url
    }

    suspend fun requestPhysicalCard(name: String, address: String) = io {
        val sb = requireClient()
        val userId = sb.auth.currentUserOrNull()?.id ?: throw AuthException("Sign in again.")
        val memberId = try {
            sb.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<ProfileRow>()?.memberId.orEmpty()
        } catch (e: Exception) {
            throw AuthException(humanize(e), e)
        }
        try {
            sb.from("physical_card_requests").insert(
                PhysicalCardInsert(
                    userId = userId,
                    memberId = memberId,
                    shipName = name.trim(),
                    shipAddress = address.trim(),
                ),
            )
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            if (message.contains("duplicate", ignoreCase = true) || message.contains("unique", ignoreCase = true)) {
                throw AuthException("You're already on the physical card list.")
            }
            throw AuthException(humanize(e), e)
        }
    }

    private suspend fun postFunction(name: String, body: JSONObject): JSONObject = io {
        val sb = requireClient()
        sb.auth.awaitInitialization()
        val access = sb.auth.currentAccessTokenOrNull() ?: throw AuthException("Sign in again.")
        val endpoint = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/$name"
        val connection = (URI(endpoint).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $access")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 20_000
        }
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.readText()
            .orEmpty()
        val parsed = runCatching { JSONObject(text) }.getOrNull()
        if (code !in 200..299) {
            throw AuthException(parsed?.optString("error").orEmpty().ifBlank {
                if (code == 401) "Sign in again." else "Couldn't refresh the pass."
            })
        }
        parsed ?: throw AuthException("Couldn't refresh the pass.")
    }

    suspend fun registerDeviceToken(token: String) = io {
        requireClient().postgrest.rpc("register_device_token", DeviceTokenArg(token))
    }

    suspend fun forgetDeviceToken(token: String) = io {
        requireClient().postgrest.rpc("forget_device_token", DeviceTokenArg(token))
    }

    suspend fun fetchNotifications(): List<NoticeRow> = io {
        if (!hasCloud) return@io emptyList()
        val sb = client ?: return@io emptyList()
        sb.auth.awaitInitialization()
        if (sb.auth.currentUserOrNull() == null) return@io emptyList()
        sb.from("notifications").select(Columns.list("id", "title", "body", "created_at")) {
            order("created_at", Order.DESCENDING)
            limit(40)
        }.decodeList()
    }

    suspend fun signOut() = io {
        val sb = client ?: return@io
        runCatching {
            sb.auth.awaitInitialization()
            sb.auth.signOut()
        }.onFailure {
            runCatching { sb.auth.clearSession() }
        }
    }

    private fun requireClient(): SupabaseClient {
        if (!configured) throw AuthException("HINVR isn’t connected. Try again.")
        return client ?: throw AuthException("HINVR isn’t connected. Try again.")
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

}

class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

private fun humanize(e: Throwable): String {
    if (e is AuthWeakPasswordException) {
        return "Choose a stronger password. At least 6 characters."
    }
    val rest = e as? RestException
    val auth = e as? AuthRestException
    val message = rest?.message ?: e.message.orEmpty()
    val code = auth?.error ?: ""
    val status = rest?.statusCode ?: 0
    return when {
        code.contains("user_already_exists", ignoreCase = true) ||
            message.contains("already registered", ignoreCase = true) ||
            message.contains("already been registered", ignoreCase = true) ->
            "That email already has an account. Sign in instead."
        code.contains("invalid_credentials", ignoreCase = true) ||
            message.contains("invalid login", ignoreCase = true) ||
            message.contains("invalid credentials", ignoreCase = true) ->
            "That email or password doesn’t match."
        code.contains("otp_expired", ignoreCase = true) ||
            message.contains("has expired", ignoreCase = true) ||
            message.contains("code verifier", ignoreCase = true) ||
            message.contains("invalid or has expired", ignoreCase = true) ->
            "That link has expired. Request a new one on this phone."
        message.contains("email", ignoreCase = true) && message.contains("invalid", ignoreCase = true) ->
            "That email doesn’t look right."
        message.contains("password", ignoreCase = true) &&
            (message.contains("least", ignoreCase = true) || message.contains("weak", ignoreCase = true)) ->
            "Choose a stronger password. At least 6 characters."
        status == 429 || message.contains("rate", ignoreCase = true) ->
            "Too many tries. Wait a minute, then try again."
        message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("failed to connect", ignoreCase = true) ||
            message.contains("Unable to connect", ignoreCase = true) ->
            "No network. Try again."
        message.isNotBlank() -> message
        else -> "Couldn’t reach HINVR. Try again."
    }
}

private fun redirectRejected(e: Throwable): Boolean {
    val rest = e as? RestException
    val message = rest?.message ?: e.message.orEmpty()
    return message.contains("redirect", ignoreCase = true)
}

private fun queryParam(source: String, name: String): String? {
    val raw = Regex("(?:^|[?&#])$name=([^&#\\s]+)").find(source)?.groupValues?.getOrNull(1) ?: return null
    return runCatching { URLDecoder.decode(raw, Charsets.UTF_8) }.getOrDefault(raw)
}

private const val PASSWORD_RESET_SCHEME = "hinvr"
private const val PASSWORD_RESET_HOST = "reset"
private const val PASSWORD_RESET_URL = "$PASSWORD_RESET_SCHEME://$PASSWORD_RESET_HOST"

private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
