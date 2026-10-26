package com.hinvr.quest

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.util.concurrent.Executors
import org.json.JSONObject

/** One crash, as written to disk when it happens and sent on a later launch. */
data class CrashReport(
    val appVersion: String,
    val kind: String,
    val message: String,
    val trace: String,
    val happenedAtMs: Long,
)

/** Turning a crash into its small file and back, without the headset. */
object CrashFormat {
    const val TraceLimit = 8_000
    const val MessageLimit = 500

    fun of(error: Throwable, appVersion: String, atMs: Long): CrashReport = CrashReport(
        appVersion = appVersion,
        kind = error.javaClass.name,
        message = trim(error.message.orEmpty(), MessageLimit),
        trace = trim(trace(error), TraceLimit),
        happenedAtMs = atMs,
    )

    /** The stack trace with its causes, as Java prints it. */
    fun trace(error: Throwable): String {
        val out = StringWriter()
        PrintWriter(out).use { error.printStackTrace(it) }
        return out.toString()
    }

    /** At most [limit] characters, cut on a line where it can be, saying that it was cut. */
    fun trim(text: String, limit: Int): String {
        if (text.length <= limit) return text
        val marker = "\n…"
        val room = limit - marker.length
        val cut = text.lastIndexOf('\n', room).takeIf { it > room / 2 } ?: room
        return text.substring(0, cut) + marker
    }

    fun toJson(report: CrashReport): String = JSONObject()
        .put("app_version", report.appVersion)
        .put("kind", report.kind)
        .put("message", report.message)
        .put("trace", report.trace)
        .put("happened_at_ms", report.happenedAtMs)
        .toString()

    fun fromJson(text: String): CrashReport? = runCatching {
        val json = JSONObject(text)
        CrashReport(
            appVersion = json.getString("app_version"),
            kind = json.getString("kind"),
            message = json.optString("message"),
            trace = json.optString("trace"),
            happenedAtMs = json.getLong("happened_at_ms"),
        )
    }.getOrNull()
}

/**
 * Keeps a crash on disk as the app goes down, then sends it on a later launch once the headset
 * is paired. Never in the way: writing is a few hundred bytes, sending is off the main thread.
 */
object CrashReports {
    private const val Tag = "HinvrCrash"
    private const val MaxPerLaunch = 3
    private const val KeepDays = 14L
    private const val SendAfterMs = 15_000L

    private val main = Handler(Looper.getMainLooper())
    private val network = Executors.newSingleThreadExecutor()
    private var dir: File? = null
    private var sendScheduled = false

    fun install(context: Context) {
        val folder = File(context.applicationContext.filesDir, "crashes").also { it.mkdirs() }
        dir = folder
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val report = CrashFormat.of(error, BuildConfig.VERSION_NAME, System.currentTimeMillis())
                File(folder, "crash-${report.happenedAtMs}.json").writeText(CrashFormat.toJson(report))
            }
            // The app goes down exactly as it would have.
            previous?.uncaughtException(thread, error)
        }
    }

    /** Once a launch, a little after start, when there is a paired account to send to. */
    fun sendLater() {
        if (sendScheduled) return
        sendScheduled = true
        main.postDelayed({ send() }, SendAfterMs)
    }

    private fun send() {
        val folder = dir ?: return
        if (!QuestAccount.configured || !QuestAccount.isPaired) return
        network.execute {
            val cutoff = System.currentTimeMillis() - KeepDays * 24 * 60 * 60_000L
            val files = folder.listFiles { f -> f.name.endsWith(".json") }.orEmpty().sortedBy { it.name }
            files.filter { it.lastModified() < cutoff }.forEach { it.delete() }
            for (file in files.filter { it.exists() }.take(MaxPerLaunch)) {
                val report = CrashFormat.fromJson(runCatching { file.readText() }.getOrDefault(""))
                if (report == null) {
                    file.delete()
                    continue
                }
                val outcome = runCatching {
                    QuestAccount.call(
                        "report_vr_crash",
                        QuestAccount.deviceArgs()
                            .put("p_app_version", report.appVersion)
                            .put("p_kind", report.kind)
                            .put("p_message", report.message)
                            .put("p_trace", report.trace)
                            .put("p_happened_at", Instant.ofEpochMilli(report.happenedAtMs).toString()),
                    )
                }
                val error = outcome.exceptionOrNull()
                when {
                    error == null -> file.delete()
                    // No such function yet, too many at once, or the server is down: keep it for later.
                    error is QuestAccount.ServerRefused && (error.status == 404 || error.status == 429 || error.status >= 500) ->
                        return@execute
                    // Refused for good (bad data): sending again won't help.
                    error is QuestAccount.ServerRefused -> file.delete()
                    // Offline: keep it, try next launch.
                    else -> {
                        Log.w(Tag, "Crash report not sent", error)
                        return@execute
                    }
                }
            }
        }
    }
}
