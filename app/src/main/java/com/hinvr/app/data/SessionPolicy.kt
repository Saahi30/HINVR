package com.hinvr.app.data

/**
 * What one sync learned from the server. The pass has to survive a weak signal at
 * the temple gate, so only [SignedOut] may ever clear a member's local state.
 */
sealed interface RemoteSync {
    /** Session and profile both read. [RemoteUser.profile] null means no profile row yet. */
    data class Fresh(val user: RemoteUser) : RemoteSync

    /** The session is good but the profile read failed on the network. */
    data class SessionOnly(val id: String, val email: String, val phone: String) : RemoteSync

    /** Auth could not be reached, or is still retrying a refresh. Nothing is known. */
    data object Offline : RemoteSync

    /** The server has no session for this phone: never signed in, refresh rejected, or user deleted. */
    data object SignedOut : RemoteSync
}

enum class SyncAction {
    /** Leave every local value, including tier and pass, exactly as it is. */
    KeepAll,

    /** Update account identity only; keep last-known profile, tier and pass. */
    ApplyIdentity,

    /** Replace profile and membership with what the server said. */
    ApplyProfile,

    /** The server rejected this session. Clear it and send the member to sign in. */
    ClearSession,
}

fun syncAction(remote: RemoteSync, locallySignedIn: Boolean): SyncAction = when (remote) {
    is RemoteSync.Fresh -> SyncAction.ApplyProfile
    is RemoteSync.SessionOnly -> SyncAction.ApplyIdentity
    RemoteSync.Offline -> SyncAction.KeepAll
    RemoteSync.SignedOut -> if (locallySignedIn) SyncAction.ClearSession else SyncAction.KeepAll
}

fun tierOf(raw: String?): MembershipTier =
    MembershipTier.entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: MembershipTier.None

/**
 * A fresh profile keeps the stored pass only when it still describes the same
 * credential: same desk-pass tier, same member ID. A renewal date change does not
 * retire a code that is still inside its signed lifetime.
 */
fun passSurvivesProfile(
    localTier: String,
    localMemberId: String,
    remoteTier: String,
    remoteMemberId: String,
): Boolean {
    val remote = tierOf(remoteTier)
    return remote.hasDeskPass &&
        remote == tierOf(localTier) &&
        remoteMemberId.trim() == localMemberId.trim()
}

/** Pass codes live six hours. Re-issue early, while online, so a gate visit never meets a dead code. */
const val PassReissueThresholdMs: Long = 3L * 60 * 60 * 1000

fun passIsUsable(token: String, expiresAt: Long, now: Long): Boolean =
    token.startsWith(PassTokenPrefix) && expiresAt > now

fun passNeedsReissue(
    token: String,
    expiresAt: Long,
    now: Long,
    threshold: Long = PassReissueThresholdMs,
): Boolean = !passIsUsable(token, expiresAt, now) || expiresAt - now < threshold

const val PassTokenPrefix = "HNV1."
