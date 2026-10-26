package com.hinvr.quest

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class SoundLevel(val gain: Float, @StringRes val label: Int) {
    Full(1f, R.string.sound_full),
    Low(0.35f, R.string.sound_low),
    Off(0f, R.string.sound_off),
}

/** One volume for the whole app, plus ducking while a darshan stream plays. */
object AudioMix {
    private var prefs: SharedPreferences? = null

    var level by mutableStateOf(SoundLevel.Full)
        private set

    /** A stream panel reports when its player is actually playing. */
    var streamPlaying by mutableStateOf(false)

    /** Paused app or lost input focus: nothing of ours should be audible. */
    var held by mutableStateOf(false)
        private set

    fun attach(context: Context) {
        val store = context.applicationContext.getSharedPreferences("hinvr_quest_prefs", Context.MODE_PRIVATE)
        prefs = store
        level = SoundLevel.entries.firstOrNull { it.name == store.getString("sound", null) } ?: SoundLevel.Full
    }

    fun cycle() {
        level = SoundLevel.entries[(level.ordinal + 1) % SoundLevel.entries.size]
        prefs?.edit { putString("sound", level.name) }
        WebPlayers.applyVolume()
    }

    fun hold(held: Boolean) {
        if (this.held == held) return
        this.held = held
        SanctumSound.onHold(held)
        MandirSound.onHold(held)
    }

    /** Ambience sits under a stream instead of fighting it. */
    val ambienceGain: Float get() = level.gain * (if (streamPlaying) 0.15f else 1f)
}

/**
 * A seamless loop of one asset clip: two players, and the next starts while the last one
 * fades, so the file's own fade-out never leaves a gap. Prepared off the render tick.
 */
class LoopPlayer(
    private val assets: AssetManager,
    private val assetPath: String,
    private val crossfadeMs: Int,
    private val balance: () -> Float = { 0f },
    private val volume: () -> Float,
) {
    private val main = Handler(Looper.getMainLooper())
    private val players = arrayOfNulls<MediaPlayer>(2)
    private val prepared = BooleanArray(2)
    private var current = 0
    private var fadeStartedAt = 0L
    private var wanted = false
    private var paused = false
    private var released = false
    private var level = 0f

    private val tick = object : Runnable {
        override fun run() {
            step()
            if (wanted && !paused && !released) main.postDelayed(this, 50L)
        }
    }

    fun play() {
        if (released) return
        wanted = true
        paused = false
        if (players[0] == null) {
            players[0] = create(0)
            players[1] = create(1)
        }
        restartTick()
    }

    fun pause() {
        paused = true
        main.removeCallbacks(tick)
        players.forEach { runCatching { if (it?.isPlaying == true) it.pause() } }
    }

    fun resume() {
        if (!wanted || released) return
        paused = false
        val player = players[current]
        if (player != null && prepared[current]) runCatching { player.start() }
        if (fadeStartedAt != 0L) players[1 - current]?.let { runCatching { it.start() } }
        restartTick()
    }

    fun stop() {
        wanted = false
        paused = false
        main.removeCallbacks(tick)
        players.forEach { runCatching { if (it?.isPlaying == true) it.pause() } }
        players.forEachIndexed { index, player -> if (prepared[index]) runCatching { player?.seekTo(0) } }
        fadeStartedAt = 0L
        level = 0f
    }

    fun release() {
        released = true
        wanted = false
        main.removeCallbacks(tick)
        players.forEach { runCatching { it?.release() } }
        players.fill(null)
    }

    private fun restartTick() {
        main.removeCallbacks(tick)
        main.post(tick)
    }

    private fun create(slot: Int): MediaPlayer? = runCatching {
        val fd = assets.openFd(assetPath)
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            fd.use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            setVolume(0f, 0f)
            setOnPreparedListener {
                prepared[slot] = true
                if (slot == current && wanted && !paused && !released) restartTick()
            }
            setOnErrorListener { _, what, extra ->
                Log.w("HinvrLoop", "$assetPath failed: $what/$extra")
                prepared[slot] = false
                true
            }
            prepareAsync()
        }
    }.onFailure { Log.w("HinvrLoop", "Couldn't open $assetPath", it) }.getOrNull()

    private fun step() {
        val player = players[current] ?: return
        if (!prepared[current]) return
        val target = volume().coerceIn(0f, 1f)
        // Ease toward the target so ducking and the level toggle never click.
        level += (target - level) * 0.25f
        if (!player.isPlaying && fadeStartedAt == 0L) runCatching { player.start() }
        val duration = runCatching { player.duration }.getOrDefault(0)
        val position = runCatching { player.currentPosition }.getOrDefault(0)
        val next = players[1 - current]
        if (fadeStartedAt == 0L && duration > 0 && duration - position <= crossfadeMs && next != null && prepared[1 - current]) {
            runCatching {
                next.seekTo(0)
                next.setVolume(0f, 0f)
                next.start()
            }
            fadeStartedAt = SystemClock.elapsedRealtime()
        }
        val pan = balance().coerceIn(-1f, 1f)
        if (fadeStartedAt == 0L) {
            setPanned(player, level, pan)
            return
        }
        val t = ((SystemClock.elapsedRealtime() - fadeStartedAt) / crossfadeMs.toFloat()).coerceIn(0f, 1f)
        // Equal power, so the overlap doesn't swell.
        setPanned(player, level * sqrt(1f - t), pan)
        next?.let { setPanned(it, level * sqrt(t), pan) }
        if (t >= 1f || !player.isPlaying) {
            runCatching {
                player.pause()
                player.seekTo(0)
            }
            current = 1 - current
            fadeStartedAt = 0L
        }
    }

    /** Equal-power pan: -1 is all left, 1 all right, 0 both ears at [gain]. */
    private fun setPanned(player: MediaPlayer, gain: Float, pan: Float) {
        val angle = (pan + 1f) * (Math.PI / 4.0)
        val left = (gain * cos(angle) * Sqrt2).toFloat().coerceIn(0f, 1f)
        val right = (gain * sin(angle) * Sqrt2).toFloat().coerceIn(0f, 1f)
        runCatching { player.setVolume(left, right) }
    }

    private companion object {
        val Sqrt2 = sqrt(2.0)
    }
}
