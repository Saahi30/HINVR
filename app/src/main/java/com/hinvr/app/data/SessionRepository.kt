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

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore("hinvr_session")

enum class MembershipTier { None, Darshan, Gold, Platinum, Nri }

enum class Audience { Me, Parents, Family }

data class SessionSnapshot(
    val hasOnboarded: Boolean = false,
    val isLoggedIn: Boolean = false,
    val profileComplete: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val phoneE164: String = "",
    val city: String = "",
    val places: List<MemberPlace> = emptyList(),
    val languageTag: String = "en",
    val audience: Audience = Audience.Me,
    val tier: MembershipTier = MembershipTier.None,
    val memberId: String = "",
    val validUntilLabel: String = "",
    val userId: String = "",
    val favoriteMandirs: Set<String> = emptySet(),
    val localRequests: List<String> = emptyList(),
)

class SessionRepository(
    context: Context,
    private val supabase: SupabaseBackend,
) {

    private val store = context.applicationContext.sessionStore

    val configured: Boolean get() = supabase.configured

    val hasCloud: Boolean get() = supabase.hasCloud

    val snapshot: Flow<SessionSnapshot> = store.data.map { prefs ->
        SessionSnapshot(
            hasOnboarded = prefs[Keys.hasOnboarded] == true,
            isLoggedIn = prefs[Keys.isLoggedIn] == true,
            profileComplete = prefs[Keys.profileComplete] == true,
            displayName = prefs[Keys.displayName].orEmpty(),
            email = prefs[Keys.email].orEmpty(),
            phoneE164 = prefs[Keys.phoneE164].orEmpty(),
            city = prefs[Keys.city].orEmpty(),
            places = decodePlaces(prefs[Keys.places].orEmpty()).ifEmpty {
                decodePlaces(prefs[Keys.address].orEmpty())
            },
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
            favoriteMandirs = prefs[Keys.favoriteMandirs]
                .orEmpty()
                .split(",")
                .filter { it.isNotBlank() }
                .toSet(),
            localRequests = prefs[Keys.localRequests]
                .orEmpty()
                .lines()
                .filter { it.isNotBlank() },
        )
    }

    suspend fun completeOnboarding() {
        store.edit { it[Keys.hasOnboarded] = true }
    }

    /**
     * Creates a live Supabase account.
     * @return true if profile is already complete (skip setup).
     */
    suspend fun createAccount(email: String, password: String): Boolean {
        supabase.signUp(email.trim(), password)
        return adoptCurrentUser()
    }

    /**
     * @return true if profile is already complete (skip setup).
     */
    suspend fun signIn(email: String, password: String): Boolean {
        supabase.signIn(email.trim(), password)
        return adoptCurrentUser()
    }

    suspend fun completeProfile(
        name: String,
        city: String,
        languageTag: String,
        audience: Audience,
        phoneE164: String,
        places: List<MemberPlace>,
    ) {
        supabase.saveProfile(name, city, languageTag, audience, phoneE164, places)
        store.edit {
            it[Keys.profileComplete] = true
            it[Keys.displayName] = name
            it[Keys.city] = city
            it[Keys.places] = encodePlaces(places)
            it[Keys.languageTag] = languageTag
            it[Keys.audience] = audience.name
            it[Keys.phoneE164] = phoneE164
        }
    }

    suspend fun addPlace(place: MemberPlace) {
        val current = snapshot.first()
        completeProfile(
            current.displayName,
            current.city,
            current.languageTag,
            current.audience,
            current.phoneE164,
            current.places + place,
        )
    }

    suspend fun membershipPageUrl(): String = supabase.membershipPageUrl()

    suspend fun setTier(tier: MembershipTier, memberId: String, validUntilLabel: String) {
        supabase.saveTier(tier, memberId, validUntilLabel)
        store.edit {
            it[Keys.tier] = tier.name
            it[Keys.memberId] = memberId
            it[Keys.validUntil] = validUntilLabel
        }
    }

    suspend fun toggleFavorite(mandirId: String) {
        store.edit { prefs ->
            val current = prefs[Keys.favoriteMandirs]
                .orEmpty()
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (!current.add(mandirId)) current.remove(mandirId)
            prefs[Keys.favoriteMandirs] = current.sorted().joinToString(",")
        }
    }

    suspend fun addLocalRequest(kind: String, summary: String, mandirId: String? = null) {
        val sanitized = summary.replace("\n", " ").trim()
        if (sanitized.isBlank()) return
        val city = snapshot.first().city
        store.edit { prefs ->
            val existing = prefs[Keys.localRequests].orEmpty()
            prefs[Keys.localRequests] = (existing.lines().filter { it.isNotBlank() } +
                "$kind · $sanitized").takeLast(20).joinToString("\n")
        }
        runCatching { supabase.createDeskRequest(kind, sanitized, city, mandirId) }
    }

    suspend fun signOut() {
        supabase.signOut()
        store.edit { prefs ->
            val onboarded = prefs[Keys.hasOnboarded] == true
            prefs.clear()
            prefs[Keys.hasOnboarded] = onboarded
        }
    }

    /** Pull Supabase session into DataStore. Keeps the local session if the network fails. */
    suspend fun syncRemote() {
        if (!supabase.configured) return
        val user = try {
            supabase.currentUser()
        } catch (_: Exception) {
            return
        }
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
        applyUser(user)
    }

    private suspend fun adoptCurrentUser(): Boolean {
        val user = supabase.currentUser() ?: throw AuthException("Sign in again.")
        applyUser(user)
        return snapshot.first().profileComplete
    }

    private suspend fun applyUser(user: RemoteUser) {
        val profile = user.profile
        val profileComplete = profile?.profileComplete == true ||
            (user.displayName.isNotBlank() && profile?.city?.isNotBlank() == true)
        store.edit {
            it[Keys.isLoggedIn] = true
            it[Keys.hasOnboarded] = true
            it[Keys.email] = user.email
            it[Keys.phoneE164] = user.phone
            it[Keys.userId] = user.id
            it[Keys.displayName] = user.displayName
            it[Keys.city] = profile?.city.orEmpty()
            it[Keys.places] = encodePlaces(profile?.addresses.orEmpty())
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
        val email = stringPreferencesKey("email")
        val phoneE164 = stringPreferencesKey("phone_e164")
        val city = stringPreferencesKey("city")
        val address = stringPreferencesKey("address")
        val places = stringPreferencesKey("places")
        val languageTag = stringPreferencesKey("language_tag")
        val audience = stringPreferencesKey("audience")
        val tier = stringPreferencesKey("tier")
        val memberId = stringPreferencesKey("member_id")
        val validUntil = stringPreferencesKey("valid_until")
        val userId = stringPreferencesKey("user_id")
        val favoriteMandirs = stringPreferencesKey("favorite_mandirs")
        val localRequests = stringPreferencesKey("local_requests")
    }
}
