package com.hinvr.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore("hinvr_session")

enum class MembershipTier { None, Darshan, Gold, Platinum, Nri }

enum class Audience { Me, Parents, Family }

data class SessionSnapshot(
    val hasOnboarded: Boolean = false,
    val isLoggedIn: Boolean = false,
    val profileComplete: Boolean = false,
    val displayName: String = "",
    val phoneE164: String = "",
    val city: String = "",
    val languageTag: String = "en",
    val audience: Audience = Audience.Me,
    val tier: MembershipTier = MembershipTier.None,
    val memberId: String = "",
    val validUntilLabel: String = "",
    val userId: String = "",
)

data class PendingOtp(
    val userId: String,
    val phoneE164: String,
)

class SessionRepository(
    context: Context,
    private val supabase: SupabaseBackend,
) {

    private val store = context.applicationContext.sessionStore
    private val otpLock = Mutex()
    @Volatile
    var pendingOtp: PendingOtp? = null
        private set

    val configured: Boolean get() = supabase.configured

    val snapshot: Flow<SessionSnapshot> = store.data.map { prefs ->
        SessionSnapshot(
            hasOnboarded = prefs[Keys.hasOnboarded] == true,
            isLoggedIn = prefs[Keys.isLoggedIn] == true,
            profileComplete = prefs[Keys.profileComplete] == true,
            displayName = prefs[Keys.displayName].orEmpty(),
            phoneE164 = prefs[Keys.phoneE164].orEmpty(),
            city = prefs[Keys.city].orEmpty(),
            languageTag = prefs[Keys.languageTag] ?: "en",
            audience = runCatching {
                Audience.valueOf(prefs[Keys.audience] ?: Audience.Me.name)
            }.getOrDefault(Audience.Me),
            tier = runCatching {
                MembershipTier.valueOf(prefs[Keys.tier] ?: MembershipTier.None.name)
            }.getOrDefault(MembershipTier.None),
            memberId = prefs[Keys.memberId].orEmpty(),
            validUntilLabel = prefs[Keys.validUntil].orEmpty(),
            userId = prefs[Keys.userId].orEmpty(),
        )
    }

    suspend fun completeOnboarding() {
        store.edit { it[Keys.hasOnboarded] = true }
    }

    suspend fun sendPhoneOtp(phoneE164: String) {
        val userId = supabase.sendPhoneOtp(phoneE164)
        otpLock.withLock {
            pendingOtp = PendingOtp(userId = userId, phoneE164 = phoneE164)
        }
    }

    /**
     * @return true if profile is already complete (skip setup).
     */
    suspend fun verifyOtp(secret: String): Boolean {
        val pending = otpLock.withLock { pendingOtp }
            ?: throw AuthException("Start again from your number.")
        supabase.verifyOtp(pending.phoneE164, secret)
        val user = supabase.currentUser()
        applyUser(user, pending.phoneE164, pending.userId)
        otpLock.withLock { pendingOtp = null }
        return snapshot.first().profileComplete
    }

    suspend fun completeProfile(
        name: String,
        city: String,
        languageTag: String,
        audience: Audience,
    ) {
        supabase.saveProfile(name, city, languageTag, audience)
        store.edit {
            it[Keys.profileComplete] = true
            it[Keys.displayName] = name
            it[Keys.city] = city
            it[Keys.languageTag] = languageTag
            it[Keys.audience] = audience.name
        }
    }

    suspend fun setTier(tier: MembershipTier, memberId: String, validUntilLabel: String) {
        supabase.saveTier(tier, memberId, validUntilLabel)
        store.edit {
            it[Keys.tier] = tier.name
            it[Keys.memberId] = memberId
            it[Keys.validUntil] = validUntilLabel
        }
    }

    suspend fun signOut() {
        supabase.signOut()
        store.edit { prefs ->
            val onboarded = prefs[Keys.hasOnboarded] == true
            prefs.clear()
            prefs[Keys.hasOnboarded] = onboarded
        }
    }

    /** Pull Supabase session into DataStore. Safe if Cloud is unset or offline. */
    suspend fun syncRemote() {
        if (!supabase.configured) return
        val user = runCatching { supabase.currentUser() }.getOrNull()
        if (user == null) {
            val local = snapshot.first()
            if (local.isLoggedIn) {
                store.edit { prefs ->
                    val onboarded = prefs[Keys.hasOnboarded] == true
                    prefs.clear()
                    prefs[Keys.hasOnboarded] = onboarded
                }
            }
            return
        }
        applyUser(user, user.phone.ifBlank { snapshot.first().phoneE164 }, user.id)
    }

    private suspend fun applyUser(
        user: RemoteUser?,
        phoneE164: String,
        userId: String,
    ) {
        val profile = user?.profile
        val profileComplete = profile?.profileComplete == true ||
            (user?.displayName?.isNotBlank() == true && profile?.city?.isNotBlank() == true)
        store.edit {
            it[Keys.isLoggedIn] = true
            it[Keys.hasOnboarded] = true
            it[Keys.phoneE164] = phoneE164
            it[Keys.userId] = user?.id ?: userId
            it[Keys.displayName] = user?.displayName.orEmpty()
            it[Keys.city] = profile?.city.orEmpty()
            it[Keys.languageTag] = profile?.languageTag ?: "en"
            it[Keys.audience] = profile?.audience ?: Audience.Me.name
            it[Keys.profileComplete] = profileComplete
            it[Keys.tier] = profile?.tier ?: MembershipTier.None.name
            it[Keys.memberId] = profile?.memberId.orEmpty()
            it[Keys.validUntil] = profile?.validUntil.orEmpty()
        }
    }

    private object Keys {
        val hasOnboarded = booleanPreferencesKey("has_onboarded")
        val isLoggedIn = booleanPreferencesKey("is_logged_in")
        val profileComplete = booleanPreferencesKey("profile_complete")
        val displayName = stringPreferencesKey("display_name")
        val phoneE164 = stringPreferencesKey("phone_e164")
        val city = stringPreferencesKey("city")
        val languageTag = stringPreferencesKey("language_tag")
        val audience = stringPreferencesKey("audience")
        val tier = stringPreferencesKey("tier")
        val memberId = stringPreferencesKey("member_id")
        val validUntil = stringPreferencesKey("valid_until")
        val userId = stringPreferencesKey("user_id")
    }
}
