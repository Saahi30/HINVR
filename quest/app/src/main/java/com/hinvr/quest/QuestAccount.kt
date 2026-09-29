package com.hinvr.quest

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.HttpURLConnection
import java.net.URI
import java.security.SecureRandom
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
)

enum class PairStep { Offline, NeedCamera, Looking, Claiming, Welcome }

/** The phone's pairing QR. Keep it in step with the phone app's VrHeadsetsScreen. */
private const val PairPrefix = "HINVR1:"
private const val RefreshEveryMs = 120_000L

/**
 * Pairs this headset with a member account. The phone shows a short-lived QR code;
 * the headset trades it for a link and proves itself afterwards with its own secret.
 */
object QuestAccount {
    var profile by mutableStateOf<MemberProfile?>(null)
        private set

    val isPaired: Boolean get() = profile != null

    var step by mutableStateOf(PairStep.Looking)
        private set

    /** Why the last scan didn't pair, shown under the steps. */
    var note by mutableStateOf<String?>(null)
        private set

    var unpairing by mutableStateOf(false)
        private set

    var unpairError by mutableStateOf<String?>(null)
        private set

    val configured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    /** Only the pairing screen with nothing in flight needs the camera. */
    val wantsCamera: Boolean get() = !isPaired && (step == PairStep.Looking || step == PairStep.NeedCamera)

    private val main = Handler(Looper.getMainLooper())
    private val network = Executors.newSingleThreadExecutor()
    private var prefs: SharedPreferences? = null
    private var deviceId = ""
    private var secret = ""
    private var failedCode: String? = null
    private var resumed = false

    fun attach(context: Context) {
        val store = context.applicationContext.getSharedPreferences("hinvr_quest_account", Context.MODE_PRIVATE)
        prefs = store
        deviceId = store.getString(KeyDevice, null) ?: UUID.randomUUID().toString()
        secret = store.getString(KeySecret, null) ?: newSecret()
        store.edit().putString(KeyDevice, deviceId).putString(KeySecret, secret).apply()
        profile = store.getString(KeyProfile, null)?.let { runCatching { parseProfile(JSONObject(it)) }.getOrNull() }
        step = if (configured) PairStep.Looking else PairStep.Offline
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
            note = "That isn’t a HINVR pairing code. Open Profile → VR headsets on your phone."
            return
        }
        val code = raw.substring(PairPrefix.length).trim().uppercase()
        if (code == failedCode) return
        step = PairStep.Claiming
        note = null
        network.execute {
            val result = runCatching {
                rpc(
                    "claim_vr_pairing",
                    JSONObject()
                        .put("p_code", code)
                        .put("p_device_id", deviceId)
                        .put("p_secret", secret)
                        .put("p_name", deviceName()),
                )
            }
            main.post {
                val linked = result.getOrNull()?.let { runCatching { JSONObject(it) }.getOrNull() }
                if (linked == null) {
                    failedCode = code
                    note = result.exceptionOrNull()?.message ?: "Couldn’t pair. Show a new code on your phone."
                    step = PairStep.Looking
                    return@post
                }
                failedCode = null
                keep(linked)
                step = PairStep.Welcome
                main.postDelayed({
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
                if (result.isFailure) {
                    unpairError = result.exceptionOrNull()?.message ?: "Couldn’t reach HINVR. Try again."
                    return@post
                }
                forget()
                Ring.requirePairing()
            }
        }
    }

    private fun keep(body: JSONObject) {
        profile = parseProfile(body)
        prefs?.edit()?.putString(KeyProfile, body.toString())?.apply()
    }

    private fun forget() {
        profile = null
        note = null
        unpairError = null
        failedCode = null
        step = if (configured) PairStep.Looking else PairStep.Offline
        prefs?.edit()?.remove(KeyProfile)?.apply()
    }

    private fun parseProfile(body: JSONObject) = MemberProfile(
        displayName = body.optString("display_name"),
        email = body.optString("email"),
        city = body.optString("city"),
        tier = body.optString("tier"),
        memberId = body.optString("member_id"),
        validUntil = body.optString("valid_until"),
        deviceName = body.optString("device_name"),
        pairedAt = body.optString("paired_at"),
    )

    /** Posts to a Postgres function. Returns the raw JSON body; throws with the server's message. */
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
                throw IllegalStateException(message.ifBlank { "Couldn’t reach HINVR. Try again." })
            }
            return text.trim()
        } catch (e: java.io.IOException) {
            throw IllegalStateException("No network. Check the headset’s Wi‑Fi.", e)
        } finally {
            connection.disconnect()
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
