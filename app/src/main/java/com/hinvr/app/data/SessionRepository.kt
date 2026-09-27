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

val MembershipTier.hasDeskPass: Boolean
    get() = this == MembershipTier.Gold ||
        this == MembershipTier.Platinum ||
        this == MembershipTier.Nri

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
    val passToken: String = "",
    val passExpiresAt: Long = 0L,
    val cardStatus: String = "",
    val requestTier: String = "",
    val requestStatus: String = "",
    val requestNote: String = "",
    val checkIns: List<PassCheckIn> = emptyList(),
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
            passToken = prefs[Keys.passToken].orEmpty(),
            passExpiresAt = prefs[Keys.passExpiresAt]?.toLongOrNull() ?: 0L,
            cardStatus = prefs[Keys.cardStatus].orEmpty(),
            requestTier = prefs[Keys.requestTier].orEmpty(),
            requestStatus = prefs[Keys.requestStatus].orEmpty(),
            requestNote = prefs[Keys.requestNote].orEmpty(),
            checkIns = decodeCheckIns(prefs[Keys.checkIns].orEmpty()),
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

    /** Asks the desk to issue this plan. The pass opens after they approve it. */
    suspend fun requestPlan(tier: MembershipTier, amountInr: Int): String? {
        return try {
            supabase.requestMembership(tier, amountInr)
            syncRemote()
            null
        } catch (e: AuthException) {
            e.message ?: "Couldn't send the request."
        } catch (e: Exception) {
            e.message ?: "Couldn't send the request."
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
        applyDesk(user.id)
    }

    private suspend fun applyDesk(userId: String) {
        val desk = supabase.fetchMemberDesk(userId) ?: return
        store.edit {
            it[Keys.requestTier] = desk.request?.tier.orEmpty()
            it[Keys.requestStatus] = desk.request?.status.orEmpty()
            it[Keys.requestNote] = desk.request?.staffNote.orEmpty()
            it[Keys.checkIns] = encodeCheckIns(desk.checkIns)
            if (desk.cardStatus != null) it[Keys.cardStatus] = desk.cardStatus
        }
    }

    suspend fun ensurePass(rotate: Boolean = false) {
        val current = snapshot.first()
        if (!current.isLoggedIn || !current.tier.hasDeskPass) {
            store.edit {
                it.remove(Keys.passToken)
                it.remove(Keys.passExpiresAt)
            }
            return
        }
        val stillValid = current.passToken.startsWith("HNV1.") &&
            current.passExpiresAt > System.currentTimeMillis()
        if (!rotate && stillValid) return
        val issued = supabase.issuePass(rotate)
        store.edit {
            it[Keys.passToken] = issued.token
            it[Keys.passExpiresAt] = issued.expiresAt.toString()
        }
    }

    suspend fun googleWalletUrl(): String = supabase.googleWalletUrl()

    suspend fun requestPhysicalCard(name: String, address: String) {
        supabase.requestPhysicalCard(name, address)
        store.edit { it[Keys.cardStatus] = "waitlist" }
    }

    private suspend fun adoptCurrentUser(): Boolean {
        val user = supabase.currentUser() ?: throw AuthException("Sign in again.")
        applyUser(user)
        applyDesk(user.id)
        return snapshot.first().profileComplete
    }

    private suspend fun applyUser(user: RemoteUser) {
        val profile = user.profile
        val profileComplete = profile?.profileComplete == true ||
            (user.displayName.isNotBlank() && profile?.city?.isNotBlank() == true)
        val tier = profile?.tier ?: MembershipTier.None.name
        val memberId = profile?.memberId.orEmpty()
        val validUntil = profile?.validUntil.orEmpty()
        store.edit {
            val membershipChanged = it[Keys.tier] != tier ||
                it[Keys.memberId] != memberId ||
                it[Keys.validUntil] != validUntil
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
            it[Keys.tier] = tier
            it[Keys.memberId] = memberId
            it[Keys.validUntil] = validUntil
            val keepsPass = runCatching { MembershipTier.valueOf(tier).hasDeskPass }.getOrDefault(false)
            if (!keepsPass || membershipChanged) {
                it.remove(Keys.passToken)
                it.remove(Keys.passExpiresAt)
            }
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
        val passToken = stringPreferencesKey("pass_token")
        val passExpiresAt = stringPreferencesKey("pass_expires_at")
        val cardStatus = stringPreferencesKey("card_status")
        val requestTier = stringPreferencesKey("request_tier")
        val requestStatus = stringPreferencesKey("request_status")
        val requestNote = stringPreferencesKey("request_note")
        val checkIns = stringPreferencesKey("check_ins")
        val favoriteMandirs = stringPreferencesKey("favorite_mandirs")
        val localRequests = stringPreferencesKey("local_requests")
    }
}
