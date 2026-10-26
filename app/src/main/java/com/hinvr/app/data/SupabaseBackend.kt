package com.hinvr.app.data

import android.content.Intent
import android.util.Log
import com.hinvr.app.BuildConfig
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
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.serializer.KotlinXSerializer
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import org.json.JSONObject

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
                // coerceInputValues turns a JSON null into the field default, so a
                // nullable column the desk left empty never fails a whole decode.
                serializer = KotlinXSerializer(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    },
                )
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw authFailure(e)
        }
    }

    suspend fun signIn(email: String, password: String) = io {
        val sb = requireClient()
        try {
            sb.auth.signInWith(Email, redirectUrl = null) {
                this.email = email
                this.password = password
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw authFailure(e)
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!redirectRejected(e)) throw authFailure(e)
            try {
                sb.auth.resetPasswordForEmail(email, redirectUrl = null)
                false
            } catch (again: CancellationException) {
                throw again
            } catch (again: Exception) {
                throw authFailure(again)
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
            val opensApp = trimmed.startsWith(PASSWORD_RESET_URL, ignoreCase = true)
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw authFailure(e)
        }
    }

    suspend fun updatePassword(newPassword: String) = io {
        val sb = requireClient()
        try {
            sb.auth.updateUser {
                password = newPassword
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw authFailure(e)
        }
    }

    suspend fun hasSession(): Boolean = io {
        val sb = client ?: return@io false
        awaitAuth(sb)
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

    /**
     * Reads session and profile, and says how sure that answer is. Only an auth
     * server that has rejected this session yields [RemoteSync.SignedOut]; a slow
     * or missing network is [RemoteSync.Offline] and leaves the member's pass alone.
     */
    suspend fun sync(): RemoteSync = io {
        val sb = client ?: return@io RemoteSync.Offline
        val status = awaitAuth(sb) ?: return@io RemoteSync.Offline
        val user = when (status) {
            is SessionStatus.Authenticated -> status.session.user ?: sb.auth.currentUserOrNull()
            is SessionStatus.RefreshFailure, SessionStatus.Initializing -> return@io RemoteSync.Offline
            is SessionStatus.NotAuthenticated -> return@io RemoteSync.SignedOut
        } ?: return@io RemoteSync.Offline
        val profile = try {
            sb.from("profiles").select {
                filter { eq("id", user.id) }
            }.decodeSingleOrNull<ProfileRow>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Profile read failed; keeping the last profile.", e)
            return@io RemoteSync.SessionOnly(user.id, user.email.orEmpty(), user.phone.orEmpty())
        }
        RemoteSync.Fresh(
            RemoteUser(
                id = user.id,
                phone = profile?.phoneE164?.ifBlank { user.phone.orEmpty() } ?: user.phone.orEmpty(),
                email = user.email.orEmpty(),
                displayName = profile?.displayName.orEmpty(),
                profile = profile,
            ),
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
        try {
            sb.from("profiles").upsert(
                ProfileUpsert(
                    id = userId,
                    displayName = name,
                    city = city,
                    languageTag = languageTag,
                    audience = audience.name,
                    profileComplete = true,
                    phoneE164 = phoneE164,
                    // An empty list clears saved places; that is the member's choice.
                    addresses = addresses,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw authFailure(e)
        }
    }

    suspend fun requestMembership(tier: MembershipTier, amountInr: Int) = io {
        val sb = requireClient()
        val userId = sb.auth.currentUserOrNull()?.id
            ?: throw AuthException("Sign in again.")
        try {
            val pending = sb.from("membership_requests").select {
                filter {
                    eq("user_id", userId)
                    eq("status", "pending")
                }
                limit(1)
            }.decodeList<MembershipRequestRow>()
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw authFailure(e)
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
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            MemberDeskState(request = request, checkIns = checkIns, cardStatus = cardStatus)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /** One insert. The caller keeps the request queued on [DeskSendResult.Transient]. */
    suspend fun createDeskRequest(
        kind: String,
        summary: String,
        city: String,
        mandirId: String? = null,
    ): DeskSendResult = io {
        val sb = client ?: return@io DeskSendResult.Transient
        val status = awaitAuth(sb) ?: return@io DeskSendResult.Transient
        val userId = when (status) {
            is SessionStatus.Authenticated -> status.session.user?.id ?: sb.auth.currentUserOrNull()?.id
            is SessionStatus.NotAuthenticated -> return@io DeskSendResult.Rejected("Sign in again.")
            else -> null
        } ?: return@io DeskSendResult.Transient
        try {
            sb.from("desk_requests").insert(
                DeskRequestInsert(
                    userId = userId,
                    kind = kind,
                    summary = clampDeskSummary(summary),
                    city = city,
                    mandirId = mandirId?.ifBlank { null },
                ),
            )
            DeskSendResult.Accepted
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isTransient(e)) {
                DeskSendResult.Transient
            } else {
                Log.w(TAG, "Desk request refused.", e)
                DeskSendResult.Rejected(humanize(e))
            }
        }
    }

    /**
     * One-time browser URL that signs this member into the membership site. The
     * token rides in the fragment (#token_hash=…), so it never reaches a server log
     * or a Referer. The handoff only accepts a token minted in the last five
     * minutes, so the session is refreshed right before the call.
     */
    suspend fun membershipPageUrl(): String = io {
        val parsed = postFunction(
            "membership-handoff",
            JSONObject(),
            fallback = "Couldn't open membership.",
            forceRefresh = true,
        )
        val token = parsed.optString("token_hash")
        if (token.isBlank()) throw AuthException("Couldn't open membership.")
        val type = parsed.optString("type").ifBlank { "magiclink" }
        val base = BuildConfig.MEMBERSHIP_URL.trim().substringBefore('#')
        base + "#token_hash=" + URLEncoder.encode(token, Charsets.UTF_8.name()) +
            "&type=" + URLEncoder.encode(type, Charsets.UTF_8.name())
    }

    data class IssuedPass(val token: String, val expiresAt: Long)

    suspend fun issuePass(rotate: Boolean): IssuedPass = io {
        val body = JSONObject().put("rotate", rotate)
        val parsed = postFunction("pass-credential", body, fallback = "Couldn't refresh the pass.")
        val token = parsed.optString("token")
        val expires = parsed.optString("expires_at")
        if (token.isBlank() || !token.startsWith(PassTokenPrefix) || expires.isBlank()) {
            throw AuthException("Couldn't refresh the pass.")
        }
        val expiresAt = runCatching { java.time.Instant.parse(expires).toEpochMilli() }
            .recoverCatching { java.time.OffsetDateTime.parse(expires).toInstant().toEpochMilli() }
            .getOrElse { throw AuthException("Couldn't refresh the pass.") }
        IssuedPass(token, expiresAt)
    }

    suspend fun googleWalletUrl(): String = io {
        val parsed = postFunction("pass-wallet", JSONObject(), fallback = "Couldn't open Google Wallet.")
        val url = parsed.optString("url")
        if (!url.startsWith("https://pay.google.com/")) {
            throw AuthException(parsed.optString("error").ifBlank { "Couldn't open Google Wallet." })
        }
        url
    }

    suspend fun requestPhysicalCard(name: String, address: String) = io {
        val sb = requireClient()
        val userId = sb.auth.currentUserOrNull()?.id ?: throw AuthException("Sign in again.")
        try {
            sb.from("physical_card_requests").insert(
                PhysicalCardInsert(
                    userId = userId,
                    shipName = name.trim(),
                    shipAddress = address.trim(),
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            if (message.contains("duplicate", ignoreCase = true) || message.contains("unique", ignoreCase = true)) {
                throw AuthException("You're already on the physical card list.")
            }
            throw authFailure(e)
        }
    }

    /**
     * Edge functions take the member's access token by hand, so refresh it first
     * when it is close to expiry (or always, with [force]); supabase-kt only
     * refreshes its own requests.
     */
    private suspend fun freshAccessToken(sb: SupabaseClient, force: Boolean = false): String {
        when (val status = awaitAuth(sb)) {
            is SessionStatus.Authenticated -> {
                if (force || status.session.expiresAt - Clock.System.now() < TokenRefreshMargin) {
                    try {
                        sb.auth.refreshCurrentSession()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        throw authFailure(e)
                    }
                }
            }
            is SessionStatus.NotAuthenticated -> throw AuthException("Sign in again.")
            else -> throw AuthException("No network. Try again.", transient = true)
        }
        return sb.auth.currentAccessTokenOrNull() ?: throw AuthException("Sign in again.")
    }

    private suspend fun postFunction(
        name: String,
        body: JSONObject,
        fallback: String,
        forceRefresh: Boolean = false,
    ): JSONObject {
        val sb = requireClient()
        return try {
            postFunctionOnce(name, body, fallback, freshAccessToken(sb, forceRefresh))
        } catch (stale: StaleSessionException) {
            // The function wants a newer token than the one we sent: refresh once, retry once.
            Log.i(TAG, "$name asked for a fresher session; retrying once.")
            try {
                postFunctionOnce(name, body, fallback, freshAccessToken(sb, force = true))
            } catch (_: StaleSessionException) {
                throw AuthException("Sign in again.")
            }
        }
    }

    private fun postFunctionOnce(
        name: String,
        body: JSONObject,
        fallback: String,
        access: String,
    ): JSONObject {
        val endpoint = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/$name"
        try {
            val connection = (URI(endpoint).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $access")
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 20_000
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()
                    ?.readText()
                    .orEmpty()
                val parsed = runCatching { JSONObject(text) }.getOrNull()
                if (code !in 200..299) {
                    val serverMessage = parsed?.optString("error").orEmpty()
                    if (serverMessage.isNotBlank()) Log.w(TAG, "$name returned $code: $serverMessage")
                    throw when {
                        code == 401 && parsed?.optString("code") == "stale_session" -> StaleSessionException()
                        code == 401 -> AuthException("Sign in again.")
                        code == 429 -> AuthException("Too many tries. Wait a minute, then try again.", transient = true)
                        code >= 500 -> AuthException(fallback, transient = true)
                        else -> AuthException(serverMessage.ifBlank { fallback })
                    }
                }
                return parsed ?: throw AuthException(fallback)
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            throw AuthException("No network. Try again.", e, transient = true)
        }
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
        awaitAuth(sb)
        if (sb.auth.currentUserOrNull() == null) return@io emptyList()
        try {
            sb.from("notifications").select(Columns.list("id", "title", "body", "created_at")) {
                order("created_at", Order.DESCENDING)
                limit(40)
            }.decodeList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw authFailure(e)
        }
    }

    /** A single-use code the headset scans. */
    suspend fun startVrPairing(): VrPairingCode = io {
        try {
            requireClient().postgrest.rpc("start_vr_pairing").decodeAs<VrPairingCode>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw authFailure(e)
        }
    }

    suspend fun fetchVrDevices(): List<VrDeviceRow> = io {
        val sb = requireClient()
        val userId = sb.auth.currentUserOrNull()?.id ?: throw AuthException("Sign in again.")
        try {
            sb.from("vr_devices").select(Columns.list("device_id", "name", "paired_at", "last_seen_at")) {
                filter { eq("user_id", userId) }
                order("paired_at", Order.DESCENDING)
            }.decodeList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw authFailure(e)
        }
    }

    suspend fun unlinkVrDevice(deviceId: String) = io {
        try {
            requireClient().postgrest.rpc("unlink_vr_device", VrDeviceArg(deviceId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw authFailure(e)
        }
    }

    suspend fun signOut() = io {
        val sb = client ?: return@io
        attempt {
            awaitAuth(sb)
            sb.auth.signOut()
        }.onFailure {
            attempt { sb.auth.clearSession() }
        }
    }

    private fun requireClient(): SupabaseClient {
        if (!configured) throw AuthException("HINVR isn’t connected. Try again.")
        return client ?: throw AuthException("HINVR isn’t connected. Try again.")
    }

    /** The published catalog, or null when it could not be read. An empty list is a real answer. */
    suspend fun fetchCatalog(): RemoteCatalog? = io {
        if (!hasCloud) return@io null
        val sb = client ?: return@io null
        try {
            val mandirs = sb.from("mandirs").select {
                filter { eq("published", true) }
                order("sort_order", Order.ASCENDING)
            }.decodeList<MandirRow>()
            val services = sb.from("home_services").select {
                filter { eq("published", true) }
                order("sort_order", Order.ASCENDING)
            }.decodeList<ServiceRow>()
            val headline = try {
                sb.from("app_settings").select {
                    filter { eq("key", "home") }
                }.decodeSingleOrNull<SettingRow>()?.value?.headline?.ifBlank { null }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            RemoteCatalog(mandirs, services, headline)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Catalog read failed; keeping the last catalog.", e)
            null
        }
    }

    private companion object {
        const val TAG = "HinvrBackend"
        val TokenRefreshMargin = 60.seconds
    }
}

/**
 * A member-facing failure. [transient] marks a network or server hiccup: worth
 * retrying, and never a reason to discard the member's local pass or session.
 */
/** 401 {code:"stale_session"}: the token was valid but too old for this function. */
private class StaleSessionException : Exception("stale_session")

class AuthException(
    message: String,
    cause: Throwable? = null,
    val transient: Boolean = false,
) : Exception(message, cause)

/** Waits for supabase-kt to load and refresh the stored session, but never forever. */
private suspend fun awaitAuth(sb: SupabaseClient): SessionStatus? {
    withTimeoutOrNull(AuthInitTimeoutMs) { sb.auth.awaitInitialization() } ?: return null
    return sb.auth.sessionStatus.value
}

private const val AuthInitTimeoutMs = 8_000L

internal fun isTransient(e: Throwable): Boolean {
    if (e is IOException) return true
    if (e is AuthException) return e.transient
    val status = (e as? RestException)?.statusCode ?: return e.cause?.let(::isTransient) ?: false
    return status == 408 || status == 429 || status >= 500
}

private fun authFailure(e: Throwable): AuthException =
    AuthException(humanize(e), e, transient = isTransient(e))

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
        status == 429 || message.contains("rate limit", ignoreCase = true) ->
            "Too many tries. Wait a minute, then try again."
        status == 401 || code.contains("session_not_found", ignoreCase = true) ->
            "Sign in again."
        e is IOException ||
            message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("failed to connect", ignoreCase = true) ||
            message.contains("Unable to connect", ignoreCase = true) ->
            "No network. Try again."
        else -> {
            // Raw server text is for logcat, not for members.
            Log.w("HinvrBackend", "Unmapped error: $message", e)
            "Something went wrong. Try again."
        }
    }
}

private fun redirectRejected(e: Throwable): Boolean {
    val rest = e as? RestException
    val message = rest?.message ?: e.message.orEmpty()
    return message.contains("redirect", ignoreCase = true)
}

private fun queryParam(source: String, name: String): String? {
    val raw = Regex("(?:^|[?&#])$name=([^&#\\s]+)").find(source)?.groupValues?.getOrNull(1) ?: return null
    return runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
}

private const val PASSWORD_RESET_SCHEME = "hinvr"
private const val PASSWORD_RESET_HOST = "reset"
private const val PASSWORD_RESET_URL = "$PASSWORD_RESET_SCHEME://$PASSWORD_RESET_HOST"

private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
