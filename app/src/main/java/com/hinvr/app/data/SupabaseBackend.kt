package com.hinvr.app.data

import com.hinvr.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
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

/**
 * Supabase client for phone OTP, session, and [profiles].
 *
 * Phone SMS stays mocked while [BuildConfig.MOCK_PHONE_OTP] is true
 * (default). Set MOCK_PHONE_OTP=false in local.properties when a provider is ready.
 */
class SupabaseBackend {

    val mockPhoneOtp: Boolean = BuildConfig.MOCK_PHONE_OTP

    val configured: Boolean =
        !mockPhoneOtp &&
            BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    private val client: SupabaseClient? = if (configured) {
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
