package com.hinvr.quest

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import java.net.HttpURLConnection
import java.net.URI
import java.security.SecureRandom
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import org.json.JSONObject

/** The member this headset is linked to, as the phone app's profile shows it. */
data class MemberProfile(
    val displayName: String,
    val email: String,
    val city: String,
    val tier: String,
    val memberId: String,
    val validUntil: String,
    val deviceName: String,
    val pairedAt: String,
    /** The member's family, as added on the phone, for offerings made in their name. */
    val family: List<FamilyMember> = emptyList(),
)

/** Someone the member offers for by name. [relation] is the server's key (mother, father, …). */
data class FamilyMember(val id: String, val name: String, val relation: String)

enum class PairStep { Offline, NeedCamera, Looking, Claiming, Welcome, Paired }

/** The phone's pairing QR. Keep it in step with the phone app's VrHeadsetsScreen. */
private const val PairPrefix = "HINVR1:"
private const val RefreshEveryMs = 120_000L
private val ClaimRetryMs = longArrayOf(2_000L, 4_000L, 8_000L)

/**
 * Pairs this headset with a member account. The phone shows a short-lived QR code;
 * the headset trades it for a link and proves itself afterwards with its own secret.
 * A fresh secret is made for every pairing, so an old one stops working once re-paired.
 */
object QuestAccount {
    private const val Tag = "HinvrAccount"

    /** The headset couldn't reach the server at all. */
    class NetworkFailure(cause: Throwable) : Exception("network", cause)

    /** The server answered and said no. The message is for the log, not the member. */
    class ServerRefused(val status: Int, val serverMessage: String) : Exception(serverMessage)

    var profile by mutableStateOf<MemberProfile?>(null)
        private set

    val isPaired: Boolean get() = profile != null

    var step by mutableStateOf(PairStep.Looking)
        private set

    /** Why the last scan didn't pair, shown under the steps. */
    @get:StringRes
    var note by mutableStateOf<Int?>(null)
        private set

    var unpairing by mutableStateOf(false)
        private set

    @get:StringRes
    var unpairError by mutableStateOf<Int?>(null)
        private set

    val configured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    /** Only the pairing screen with nothing in flight needs the camera. */
    val wantsCamera: Boolean get() = !isPaired && (step == PairStep.Looking || step == PairStep.NeedCamera)

    private val main = Handler(Looper.getMainLooper())
    private val network = Executors.newSingleThreadExecutor()
    private var store: SecureStore? = null
    private var deviceId = ""
    private var secret = ""
    private var failedCode: String? = null
    private var resumed = false

    fun attach(context: Context) {
        val app = context.applicationContext
        HeadsetBattery.attach(app)
        val secure = SecureStore(app, "hinvr_quest_secure", "hinvr_quest_account")
        store = secure
        migratePlainPrefs(app, secure)
        deviceId = secure.get(KeyDevice) ?: UUID.randomUUID().toString().also { secure.put(KeyDevice, it) }
        secret = secure.get(KeySecret) ?: newSecret().also { secure.put(KeySecret, it) }
        profile = secure.get(KeyProfile)?.let { runCatching { parseProfile(JSONObject(it)) }.getOrNull() }
        step = restingStep()
    }

    /** Earlier builds kept these in plain SharedPreferences. Move them once, then wipe the file. */
    private fun migratePlainPrefs(context: Context, secure: SecureStore) {
        val plain = context.getSharedPreferences("hinvr_quest_account", Context.MODE_PRIVATE)
        if (plain.all.isEmpty()) return
        var moved = true
        listOf(KeyDevice, KeySecret, KeyProfile).forEach { key ->
            val value = plain.getString(key, null) ?: return@forEach
            if (secure.get(key) == null) moved = secure.put(key, value) && moved
        }
        if (moved) plain.edit { clear() }
    }

    private fun restingStep(): PairStep = when {
        !configured -> PairStep.Offline
        isPaired -> PairStep.Paired
        else -> PairStep.Looking
    }

    fun onResume() {
        resumed = true
        refresh()
        main.removeCallbacks(periodic)
        main.postDelayed(periodic, RefreshEveryMs)
    }

    fun onPause() {
        resumed = false
        main.removeCallbacks(periodic)
    }

    private val periodic = object : Runnable {
        override fun run() {
            if (!resumed) return
            refresh()
            main.postDelayed(this, RefreshEveryMs)
        }
    }

    /** Picks up profile edits, and notices when the phone unlinked this headset. */
    fun refresh() {
        if (!configured || !isPaired) return
        network.execute {
            val result = runCatching {
                rpc("vr_device_profile", JSONObject().put("p_device_id", deviceId).put("p_secret", secret))
            }
            result.exceptionOrNull()?.let { Log.w(Tag, "vr_device_profile failed", it) }
            main.post {
                val body = result.getOrNull() ?: return@post
                if (!isPaired) return@post
                if (body == "null" || body.isBlank()) {
                    forget()
                    Ring.requirePairing()
                } else {
                    runCatching { JSONObject(body) }.getOrNull()?.let(::keep)
                }
            }
        }
    }

    fun cameraBlocked(blocked: Boolean) {
        if (isPaired || !configured) return
        if (blocked && step == PairStep.Looking) step = PairStep.NeedCamera
        if (!blocked && step == PairStep.NeedCamera) step = PairStep.Looking
    }

    /** Called on the main thread with whatever QR the camera read. */
    fun onScanned(text: String) {
        if (isPaired || step != PairStep.Looking) return
        val raw = text.trim()
        if (!raw.startsWith(PairPrefix, ignoreCase = true)) {
            note = R.string.pair_note_not_code
            return
        }
        val code = raw.substring(PairPrefix.length).trim().uppercase()
        if (code == failedCode) return
        step = PairStep.Claiming
        note = null
        claim(code, newSecret(), attempt = 0)
    }

    private fun claim(code: String, fresh: String, attempt: Int) {
        network.execute {
            val result = runCatching {
                rpc(
                    "claim_vr_pairing",
                    JSONObject()
                        .put("p_code", code)
                        .put("p_device_id", deviceId)
                        .put("p_secret", fresh)
                        .put("p_name", deviceName()),
                )
            }
            main.post {
                if (isPaired || step != PairStep.Claiming) return@post
                val error = result.exceptionOrNull()
                val linked = result.getOrNull()?.let { runCatching { JSONObject(it) }.getOrNull() }
                if (linked == null) {
                    Log.w(Tag, "claim_vr_pairing failed (attempt ${attempt + 1})", error)
                    if (error is NetworkFailure && attempt < ClaimRetryMs.size) {
                        // The code lives a few minutes; a Wi-Fi blip shouldn't cost the member a new one.
                        main.postDelayed({
                            if (!isPaired && step == PairStep.Claiming) claim(code, fresh, attempt + 1)
                        }, ClaimRetryMs[attempt])
                        return@post
                    }
                    // Network trouble leaves the same code scannable again; a refusal doesn't.
                    if (error !is NetworkFailure) failedCode = code
                    note = friendly(error)
                    step = PairStep.Looking
                    return@post
                }
                failedCode = null
                secret = fresh
                store?.put(KeySecret, fresh)
                keep(linked)
                step = PairStep.Welcome
                main.postDelayed({
                    if (step == PairStep.Welcome) step = PairStep.Paired
                    if (Ring.stage == Stage.Pair && isPaired) Ring.openMenu()
                }, 2_200L)
            }
        }
    }

    fun unpair() {
        if (!isPaired || unpairing) return
        unpairing = true
        unpairError = null
        network.execute {
            val result = runCatching {
                rpc("unpair_vr_device", JSONObject().put("p_device_id", deviceId).put("p_secret", secret))
            }
            main.post {
                unpairing = false
                result.exceptionOrNull()?.let { error ->
                    Log.w(Tag, "unpair_vr_device failed", error)
                    unpairError = friendly(error)
                    return@post
                }
                forget()
                Ring.requirePairing()
            }
        }
    }

    private fun keep(body: JSONObject) {
        profile = parseProfile(body)
        store?.put(KeyProfile, body.toString())
        if (step != PairStep.Welcome) step = restingStep()
    }

    private fun forget() {
        profile = null
        note = null
        unpairError = null
        failedCode = null
        store?.remove(KeyProfile)
        // The old secret is spent; the next pairing brings its own.
        secret = newSecret()
        store?.put(KeySecret, secret)
        step = restingStep()
    }

    private fun parseProfile(body: JSONObject) = MemberProfile(
        displayName = body.optString("display_name"),
        email = body.optString("email"),
        city = body.optString("city"),
        tier = body.optString("tier"),
        memberId = body.optString("member_id"),
        validUntil = body.optString("valid_until").takeIf { it != "null" }.orEmpty(),
        deviceName = body.optString("device_name"),
        pairedAt = body.optString("paired_at"),
        family = parseFamily(body),
    )

    /** "family": [{id, name, relation}]; absent on a server that has none, and then nobody. */
    private fun parseFamily(body: JSONObject): List<FamilyMember> {
        val rows = body.optJSONArray("family") ?: return emptyList()
        return (0 until rows.length()).mapNotNull { i ->
            val row = rows.optJSONObject(i) ?: return@mapNotNull null
            val id = row.optString("id")
            val name = row.optString("name").trim()
            if (id.isBlank() || name.isBlank()) null else FamilyMember(id, name, row.optString("relation"))
        }
    }

    fun deviceArgs(): JSONObject =
        JSONObject().put("p_device_id", deviceId).put("p_secret", secret)

    fun call(name: String, args: JSONObject = JSONObject()): String = rpc(name, args)

    /** Posts to a Postgres function. Returns the raw JSON body. */
    private fun rpc(name: String, args: JSONObject): String {
        val endpoint = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/rpc/$name"
        val connection = (URI(endpoint).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            connection.outputStream.use { it.write(args.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.readText()
                .orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrNull().orEmpty()
                throw ServerRefused(code, message)
            }
            return text.trim()
        } catch (e: java.io.IOException) {
            throw NetworkFailure(e)
        } finally {
            connection.disconnect()
        }
    }

    /** What a member sees for a failure. The raw message stays in the log. */
    @StringRes
    fun friendly(error: Throwable?): Int {
        if (error is NetworkFailure) return R.string.err_network
        val message = (error as? ServerRefused)?.serverMessage.orEmpty().lowercase()
        return when {
            "expired" in message -> R.string.err_code_expired
            "another account" in message -> R.string.err_other_account
            "pairing code" in message -> R.string.pair_note_not_code
            "identified" in message -> R.string.err_headset_id
            else -> R.string.err_generic
        }
    }

    private fun deviceName(): String {
        val model = Build.MODEL.orEmpty().trim().ifBlank { "Quest" }
        return if (model.startsWith("Meta", ignoreCase = true)) model else "Meta $model"
    }

    private fun newSecret(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private const val KeyDevice = "device_id"
    private const val KeySecret = "device_secret"
    private const val KeyProfile = "profile"
}

/** How member fields read on the headset. Pure, for tests. */
object MemberFormat {
    private val Display = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    /** valid_until is becoming an SQL date ("2027-04-30"); older rows hold free text like "March 2027". */
    fun validUntil(raw: String): String {
        val value = raw.trim()
        if (value.isEmpty() || value == "null") return "—"
        runCatching { return Display.format(LocalDate.parse(value)) }
        runCatching { return Display.format(OffsetDateTime.parse(value).toLocalDate()) }
        runCatching { return Display.format(LocalDate.parse(value.take(10))) }
        return value
    }

    fun shortDate(iso: String, zone: java.time.ZoneId): String {
        val parsed = runCatching { OffsetDateTime.parse(iso) }.getOrNull() ?: return "—"
        return Display.format(parsed.atZoneSameInstant(zone))
    }
}
