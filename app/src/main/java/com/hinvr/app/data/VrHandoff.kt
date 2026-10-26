package com.hinvr.app.data

import java.time.Instant

/** What the headset should open. The wire names are the server's. */
enum class VrTarget(val wire: String) {
    Live("live"),
    Tour("tour"),
    Sanctum("sanctum"),
}

/** The backend's text when the member has no paired headset; the sheet offers pairing instead. */
const val NoHeadsetMessage = "Pair a headset first."

/** Where a "View in VR" request stands, as the phone shows it. */
sealed interface HandoffPhase {
    data object Asking : HandoffPhase
    data object Waiting : HandoffPhase
    data class Opened(val headset: String) : HandoffPhase
    data object Expired : HandoffPhase
    data object NoHeadset : HandoffPhase
    data class Failed(val message: String?) : HandoffPhase
}

object VrHandoffs {
    /** How often the phone asks whether the headset has taken it. */
    const val PollMs = 3_000L

    /** A mandir page sends its 360 tour when it has one, else its official stream; neither, nothing. */
    fun targetFor(hasTour: Boolean, hasStream: Boolean): VrTarget? = when {
        hasTour -> VrTarget.Tour
        hasStream -> VrTarget.Live
        else -> null
    }

    /** The phase for the latest status read. A missing status before the deadline is still waiting. */
    fun phaseFor(status: VrHandoffStatus?, expiresAt: Instant?, now: Instant): HandoffPhase = when {
        status?.claimedAt != null -> HandoffPhase.Opened(status.deviceName.orEmpty())
        status?.expired == true -> HandoffPhase.Expired
        expiresAt != null && !now.isBefore(expiresAt) -> HandoffPhase.Expired
        else -> HandoffPhase.Waiting
    }

    /** Polling stops once the headset has it or the request has run out. */
    fun settled(phase: HandoffPhase): Boolean = phase !is HandoffPhase.Asking && phase !is HandoffPhase.Waiting

    fun parseInstant(raw: String?): Instant? = raw?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?: raw?.let { runCatching { java.time.OffsetDateTime.parse(it).toInstant() }.getOrNull() }
}
