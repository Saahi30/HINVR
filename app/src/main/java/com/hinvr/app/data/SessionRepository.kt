package com.hinvr.app.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hinvr.app.i18n.AppLocale
import com.hinvr.app.push.PushTokens
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore("hinvr_session")

data class SessionSnapshot(
    /** False only until the stored session is read. Gates wait for true before deciding. */
    val loaded: Boolean = false,
    val hasOnboarded: Boolean = false,
    val isLoggedIn: Boolean = false,
    val mustResetPassword: Boolean = false,
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
    /** As the server sent it: an ISO date, or an older free-text label. Format with [formatValidUntil]. */
    val validUntil: String = "",
    val userId: String = "",
    val passToken: String = "",
    val passExpiresAt: Long = 0L,
    val cardStatus: String = "",
    val requestTier: String = "",
    val requestStatus: String = "",
    val requestNote: String = "",
    val checkIns: List<PassCheckIn> = emptyList(),
    val favoriteMandirs: Set<String> = emptySet(),
    val deskRequests: List<DeskRequest> = emptyList(),
)

class SessionRepository(
    context: Context,
    private val supabase: SupabaseBackend,
) {

    private val appContext = context.applicationContext
    private val store = appContext.sessionStore
    private val holdLanguage = AtomicBoolean(false)
    private val _storedLanguage = MutableStateFlow(AppLocale.read(appContext))
    val storedLanguage: StateFlow<String> = _storedLanguage.asStateFlow()

    init {
        AppLocale.applyProcessDefaults(_storedLanguage.value)
    }

    fun selectLanguage(tag: String) {
        holdLanguage.set(true)
        applyLanguage(tag)
    }

    fun releaseLanguageHold() {
        holdLanguage.set(false)
    }

    fun noteStoredLanguage(tag: String) {
        if (holdLanguage.get()) return
        applyLanguage(tag)
    }

    private fun applyLanguage(tag: String) {
        val normalized = AppLocale.normalize(tag)
        AppLocale.persist(appContext, normalized)
        AppLocale.applyProcessDefaults(normalized)
        _storedLanguage.value = normalized
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val authLinkMessageState = MutableStateFlow<String?>(null)
    private val syncLock = Mutex()
    private val deskLock = Mutex()
    private val passLock = Mutex()

    val configured: Boolean get() = supabase.configured

    /** Set when a recovery link fails to open a session, or the server ended the session. */
    val authLinkMessage: StateFlow<String?> = authLinkMessageState.asStateFlow()

    val hasCloud: Boolean get() = supabase.hasCloud

    /**
     * One hot copy of the stored session for every screen. It starts as an explicit
     * not-yet-[SessionSnapshot.loaded] value, so no gate mistakes the first frame
     * for a logged-out, non-member state.
     */
    val snapshot: StateFlow<SessionSnapshot> = store.data
        .map { prefs -> prefs.toSnapshot() }
        .stateIn(scope, SharingStarted.Eagerly, SessionSnapshot())

    private suspend fun current(): SessionSnapshot = snapshot.first { it.loaded }

    init {
        scope.launch {
            // A send the process never finished is not sent. Mark it waiting and try again.
            store.edit { prefs ->
                prefs.remove(Keys.legacyLocalRequests)
                val rows = decodeDeskRequests(prefs[Keys.deskRequests].orEmpty())
                if (rows.any { it.state == DeskRequestState.Sending }) {
                    prefs[Keys.deskRequests] = encodeDeskRequests(rows.map { it.recovered() })
                }
            }
        }
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

    /** @return true when the email link opens this app. */
    suspend fun requestPasswordReset(email: String): Boolean {
        return supabase.sendPasswordReset(email.trim())
    }

    suspend fun acceptRecoveryLink(link: String) {
        supabase.acceptRecoveryLink(link)
        store.edit { it[Keys.mustResetPassword] = true }
    }

    suspend fun completePasswordReset(password: String): Boolean {
        supabase.updatePassword(password)
        val complete = adoptCurrentUser()
        store.edit { it[Keys.mustResetPassword] = false }
        return complete
    }

    suspend fun hasAuthSession(): Boolean = supabase.hasSession()

    fun consumeAuthIntent(intent: Intent) {
        supabase.handleAuthCallback(
            intent,
            onRecovered = {
                scope.launch { store.edit { it[Keys.mustResetPassword] = true } }
            },
            onFailure = { message -> authLinkMessageState.value = message },
        )
    }

    fun clearAuthLinkMessage() {
        authLinkMessageState.value = null
    }

    suspend fun completeProfile(
        name: String,
        city: String,
        languageTag: String,
        audience: Audience,
        phoneE164: String,
        places: List<MemberPlace>,
    ) {
        try {
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
            applyLanguage(languageTag)
        } finally {
            releaseLanguageHold()
        }
    }

    suspend fun addPlace(place: MemberPlace) {
        val now = current()
        completeProfile(
            now.displayName,
            now.city,
            now.languageTag,
            now.audience,
            now.phoneE164,
            now.places + place,
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
        }
    }

    suspend fun toggleFavorite(mandirId: String) {
        store.edit { prefs ->
            val favourites = prefs[Keys.favoriteMandirs]
                .orEmpty()
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (!favourites.add(mandirId)) favourites.remove(mandirId)
            prefs[Keys.favoriteMandirs] = favourites.sorted().joinToString(",")
        }
    }

    /**
     * Stores the request on the phone first, then sends it. The returned copy says
     * honestly where it ended up: [DeskRequestState.Sent], waiting for a signal,
     * or refused. Waiting requests go again on resume, sync and reconnect.
     */
    suspend fun submitDeskRequest(kind: String, summary: String, mandirId: String? = null): DeskRequest? {
        if (summary.isBlank()) return null
        val request = newDeskRequest(
            id = UUID.randomUUID().toString(),
            kind = kind,
            summary = summary,
            city = current().city,
            mandirId = mandirId,
            now = System.currentTimeMillis(),
        )
        saveDeskRequest(request)
        return deskLock.withLock { send(request) }
    }

    /** The member asked to try a failed or waiting request again. */
    suspend fun retryDeskRequest(id: String): DeskRequest? {
        val found = current().deskRequests.find { it.id == id } ?: return null
        val again = found.retrying(manual = true) ?: return found
        saveDeskRequest(again)
        return deskLock.withLock { send(again) }
    }

    /** Sends everything still waiting. Safe to call often; nothing goes twice. */
    suspend fun flushDeskQueue() {
        if (!supabase.configured) return
        deskLock.withLock {
            for (waiting in current().deskRequests.filter { it.state == DeskRequestState.Waiting }) {
                val again = waiting.retrying(manual = false) ?: continue
                saveDeskRequest(again)
                if (send(again).state == DeskRequestState.Waiting) break
            }
        }
    }

    private suspend fun send(request: DeskRequest): DeskRequest {
        val result = if (supabase.configured) {
            supabase.createDeskRequest(request.kind, request.summary, request.city, request.mandirId)
        } else {
            DeskSendResult.Transient
        }
        val after = request.after(result)
        saveDeskRequest(after)
        return after
    }

    private suspend fun saveDeskRequest(request: DeskRequest) {
        store.edit { prefs ->
            val rows = decodeDeskRequests(prefs[Keys.deskRequests].orEmpty())
            prefs[Keys.deskRequests] = encodeDeskRequests(rows.upsert(request))
        }
    }

    suspend fun registerPush(token: String? = null) {
        if (!current().isLoggedIn) return
        val value = token ?: PushTokens.current(appContext) ?: return
        attempt { supabase.registerDeviceToken(value) }
    }

    suspend fun fetchNotifications(): Result<List<NoticeRow>> = attempt { supabase.fetchNotifications() }

    suspend fun startVrPairing(): VrPairingCode = supabase.startVrPairing()

    suspend fun vrDevices(): List<VrDeviceRow> = supabase.fetchVrDevices()

    suspend fun requestVrHandoff(target: VrTarget, mandirId: String?): VrHandoff =
        supabase.requestVrHandoff(target.wire, mandirId)

    suspend fun vrHandoffStatus(id: String): VrHandoffStatus? = supabase.vrHandoffStatus(id)

    suspend fun unlinkVrDevice(deviceId: String) = supabase.unlinkVrDevice(deviceId)

    /** The last [days] days of the member's sanctum offerings. */
    suspend fun sanctumOfferings(days: Long = 90): List<SanctumOffering> {
        val rows = supabase.fetchOfferings(Instant.now().minus(Duration.ofDays(days)).toString())
        // Names for offerings made for one family member; a removed member just loses the name.
        val names = if (rows.any { it.familyMemberId != null }) {
            attempt { supabase.fetchFamily() }.getOrDefault(emptyList()).associate { it.id to it.name }
        } else {
            emptyMap()
        }
        return SanctumJournal.fromRows(rows, names)
    }

    suspend fun familyMembers(): List<FamilyMemberRow> = supabase.fetchFamily()

    suspend fun familyLimits(): Map<String, Int>? = supabase.fetchFamilyLimits()

    suspend fun addFamilyMember(name: String, relation: Relation) =
        supabase.addFamilyMember(Family.cleanName(name), relation.wire)

    suspend fun updateFamilyMember(id: String, name: String, relation: Relation) =
        supabase.updateFamilyMember(id, Family.cleanName(name), relation.wire)

    suspend fun removeFamilyMember(id: String) = supabase.removeFamilyMember(id)

    private suspend fun forgetPush() {
        val value = PushTokens.current(appContext)
        if (value != null) attempt { supabase.forgetDeviceToken(value) }
        PushTokens.delete(appContext)
    }

    suspend fun signOut() {
        attempt { forgetPush() }
        supabase.signOut()
        clearLocalSession()
    }

    private suspend fun clearLocalSession() {
        store.edit { prefs ->
            val onboarded = prefs[Keys.hasOnboarded] == true
            prefs.clear()
            prefs[Keys.hasOnboarded] = onboarded
        }
    }

    /**
     * Pulls the server's view of this member into DataStore. A failed or partial
     * read keeps the last-known profile, tier and pass; only a session the server
     * rejected signs the member out here.
     */
    suspend fun syncRemote(): RemoteSync {
        if (!supabase.configured) return RemoteSync.Offline
        val remote = syncLock.withLock {
            val remote = supabase.sync()
            when (syncAction(remote, current().isLoggedIn)) {
                SyncAction.KeepAll -> Unit
                SyncAction.ApplyIdentity -> applyIdentity(remote as RemoteSync.SessionOnly)
                SyncAction.ApplyProfile -> {
                    val user = (remote as RemoteSync.Fresh).user
                    applyUser(user)
                    applyDesk(user.id)
                }
                SyncAction.ClearSession -> {
                    Log.i(TAG, "The server ended this session; signing out locally.")
                    attempt { forgetPush() }
                    supabase.signOut()
                    clearLocalSession()
                    authLinkMessageState.value = SessionEndedMessage
                }
            }
            remote
        }
        if (remote is RemoteSync.Fresh || remote is RemoteSync.SessionOnly) {
            flushDeskQueue()
            // Online with a good session: top up the pass before it gets close to expiry.
            attempt { ensurePass(rotate = false) }
                .onFailure { Log.w(TAG, "Early pass refresh failed; the current code stays.", it) }
        }
        return remote
    }

    /** Connectivity came back. Catch up quietly. */
    fun onNetworkAvailable() {
        scope.launch { attempt { syncRemote() } }
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

    /**
     * Keeps the signed code current. Without [rotate] it re-issues only when the
     * code is missing or under three hours from expiry. If that early refresh fails
     * while the old code is still valid, the old code stays and nothing is thrown.
     */
    suspend fun ensurePass(rotate: Boolean = false): Unit = passLock.withLock {
        val now = current()
        if (!now.isLoggedIn || !now.tier.hasDeskPass) {
            if (now.passToken.isNotEmpty()) clearPass()
            return@withLock
        }
        val clock = System.currentTimeMillis()
        if (!rotate && !passNeedsReissue(now.passToken, now.passExpiresAt, clock)) return@withLock
        val issued = try {
            supabase.issuePass(rotate)
        } catch (e: AuthException) {
            if (!rotate && passIsUsable(now.passToken, now.passExpiresAt, clock)) return@withLock
            throw e
        }
        store.edit {
            it[Keys.passToken] = issued.token
            it[Keys.passExpiresAt] = issued.expiresAt.toString()
        }
        Unit
    }

    private suspend fun clearPass() {
        store.edit {
            it.remove(Keys.passToken)
            it.remove(Keys.passExpiresAt)
        }
    }

    suspend fun googleWalletUrl(): String = supabase.googleWalletUrl()

    suspend fun requestPhysicalCard(name: String, address: String) {
        supabase.requestPhysicalCard(name, address)
        store.edit { it[Keys.cardStatus] = "waitlist" }
    }

    private suspend fun adoptCurrentUser(): Boolean {
        when (val remote = supabase.sync()) {
            is RemoteSync.Fresh -> {
                applyUser(remote.user)
                applyDesk(remote.user.id)
            }
            // Signed in, but the profile didn't load. Don't guess at setup; ask for a retry.
            is RemoteSync.SessionOnly, RemoteSync.Offline ->
                throw AuthException("No network. Try again.", transient = true)
            RemoteSync.SignedOut -> throw AuthException("Sign in again.")
        }
        return current().profileComplete
    }

    /** The profile read failed but the session is good: refresh identity, keep the rest. */
    private suspend fun applyIdentity(remote: RemoteSync.SessionOnly) {
        store.edit {
            it[Keys.isLoggedIn] = true
            it[Keys.hasOnboarded] = true
            it[Keys.userId] = remote.id
            if (remote.email.isNotBlank()) it[Keys.email] = remote.email
        }
    }

    private suspend fun applyUser(user: RemoteUser) {
        val profile = user.profile
        val profileComplete = profile?.profileComplete == true ||
            (user.displayName.isNotBlank() && profile?.city?.isNotBlank() == true)
        val tier = tierOf(profile?.tier).name
        val memberId = profile?.memberId.orEmpty().trim()
        val validUntil = profile?.validUntil.orEmpty().trim()
        store.edit {
            val keepsPass = passSurvivesProfile(
                localTier = it[Keys.tier].orEmpty(),
                localMemberId = it[Keys.memberId].orEmpty(),
                remoteTier = tier,
                remoteMemberId = memberId,
            )
            it[Keys.isLoggedIn] = true
            it[Keys.hasOnboarded] = true
            it[Keys.email] = user.email
            it[Keys.phoneE164] = user.phone
            it[Keys.userId] = user.id
            it[Keys.displayName] = user.displayName
            it[Keys.city] = profile?.city.orEmpty()
            it[Keys.places] = encodePlaces(profile?.addresses.orEmpty())
            it[Keys.languageTag] = profile?.languageTag?.ifBlank { null } ?: "en"
            it[Keys.audience] = profile?.audience?.ifBlank { null } ?: Audience.Me.name
            it[Keys.profileComplete] = profileComplete
            it[Keys.tier] = tier
            it[Keys.memberId] = memberId
            it[Keys.validUntil] = validUntil
            if (!keepsPass) it.dropPass()
        }
    }

    private fun MutablePreferences.dropPass() {
        remove(Keys.passToken)
        remove(Keys.passExpiresAt)
    }

    private fun Preferences.toSnapshot(): SessionSnapshot = SessionSnapshot(
        loaded = true,
        hasOnboarded = this[Keys.hasOnboarded] == true,
        isLoggedIn = this[Keys.isLoggedIn] == true,
        mustResetPassword = this[Keys.mustResetPassword] == true,
        profileComplete = this[Keys.profileComplete] == true,
        displayName = this[Keys.displayName].orEmpty(),
        email = this[Keys.email].orEmpty(),
        phoneE164 = this[Keys.phoneE164].orEmpty(),
        city = this[Keys.city].orEmpty(),
        places = decodePlaces(this[Keys.places].orEmpty()).ifEmpty {
            decodePlaces(this[Keys.address].orEmpty())
        },
        languageTag = this[Keys.languageTag] ?: "en",
        audience = Audience.entries.firstOrNull { it.name == this[Keys.audience] } ?: Audience.Me,
        tier = tierOf(this[Keys.tier]),
        memberId = this[Keys.memberId].orEmpty(),
        validUntil = this[Keys.validUntil].orEmpty(),
        userId = this[Keys.userId].orEmpty(),
        passToken = this[Keys.passToken].orEmpty(),
        passExpiresAt = this[Keys.passExpiresAt]?.toLongOrNull() ?: 0L,
        cardStatus = this[Keys.cardStatus].orEmpty(),
        requestTier = this[Keys.requestTier].orEmpty(),
        requestStatus = this[Keys.requestStatus].orEmpty(),
        requestNote = this[Keys.requestNote].orEmpty(),
        checkIns = decodeCheckIns(this[Keys.checkIns].orEmpty()),
        favoriteMandirs = this[Keys.favoriteMandirs]
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
            .toSet(),
        deskRequests = decodeDeskRequests(this[Keys.deskRequests].orEmpty())
            .sortedByDescending { it.createdAt },
    )

    private object Keys {
        val hasOnboarded = booleanPreferencesKey("has_onboarded")
        val isLoggedIn = booleanPreferencesKey("is_logged_in")
        val mustResetPassword = booleanPreferencesKey("must_reset_password")
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
        val deskRequests = stringPreferencesKey("desk_requests")

        /** One line per request, no delivery state. Replaced by [deskRequests]. */
        val legacyLocalRequests = stringPreferencesKey("local_requests")
    }

    private companion object {
        const val TAG = "HinvrSession"
        const val SessionEndedMessage = "Your session ended on this phone. Sign in again."
    }
}
