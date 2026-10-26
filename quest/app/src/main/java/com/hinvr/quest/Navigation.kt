package com.hinvr.quest

/** Where the member is. Exactly one stage at a time. */
enum class Stage { Splash, Pair, Credits, Menu, States, Live, Tour, Profile, Mandir }

enum class SphereStatus { Loading, Playing, Ended, Failed }

/** What a stream panel is showing, and what the server says about it. */
data class StreamSource(
    val url: String,
    val title: String,
    val spherical: Boolean,
    val live: LiveState,
)

/**
 * Something played on top of a card stage. At most one; it can't exist in the Mandir,
 * so a stream can't keep playing behind the sanctum.
 */
sealed interface Overlay {
    data object None : Overlay

    data class Stream(val source: StreamSource, val leaving: Boolean = false) : Overlay

    data class Sphere(val tour: HomeChoice, val status: SphereStatus = SphereStatus.Loading) : Overlay

    /** The tour went to the YouTube VR app; we come back to the tour list. */
    data object Handoff : Overlay
}

data class NavState(
    val stage: Stage = Stage.Splash,
    val overlay: Overlay = Overlay.None,
    val index: Int = 0,
    val selectedId: String? = null,
    val stateName: String? = null,
    /** Where Credits returns to. */
    val creditsFrom: Stage = Stage.Pair,
) {
    val showsCards: Boolean
        get() = overlay !is Overlay.Sphere && overlay !is Overlay.Handoff &&
            (stage == Stage.Menu || stage == Stage.States || stage == Stage.Live || stage == Stage.Tour)
}

/** Every screen change, as plain functions. Ring applies the side effects. */
object Nav {
    fun menu(state: NavState, paired: Boolean): NavState =
        NavState(stage = if (paired) Stage.Menu else Stage.Pair)

    /** [parentIndex] is where the open state sits in the state ring, for Live → States. */
    fun back(state: NavState, paired: Boolean, parentIndex: Int = 0): NavState {
        when (val overlay = state.overlay) {
            is Overlay.Stream -> return if (overlay.leaving) state else state.copy(overlay = overlay.copy(leaving = true))
            is Overlay.Sphere, Overlay.Handoff -> return state.copy(overlay = Overlay.None)
            Overlay.None -> Unit
        }
        return when (state.stage) {
            Stage.Splash, Stage.Pair, Stage.Menu -> state
            Stage.Credits -> if (state.creditsFrom == Stage.Profile && paired) {
                NavState(stage = Stage.Profile)
            } else {
                menu(state, paired)
            }
            Stage.States, Stage.Tour, Stage.Profile, Stage.Mandir -> menu(state, paired)
            Stage.Live -> backToStates(state, parentIndex)
        }
    }

    /** Opens the sanctum from anywhere, dropping whatever was playing. Members only. */
    fun mandir(state: NavState, paired: Boolean): NavState =
        if (paired) NavState(stage = Stage.Mandir) else NavState(stage = Stage.Pair)

    fun profile(state: NavState, paired: Boolean): NavState =
        if (!paired || state.overlay != Overlay.None || state.stage == Stage.Mandir) state else NavState(stage = Stage.Profile)

    /** Credits are a legal page, so they open without pairing. */
    fun credits(state: NavState): NavState =
        if (state.overlay != Overlay.None || state.stage == Stage.Mandir || state.stage == Stage.Splash) {
            state
        } else {
            NavState(stage = Stage.Credits, creditsFrom = state.stage)
        }

    fun greet(state: NavState): NavState =
        if (state.overlay != Overlay.None) state else NavState(stage = Stage.Splash)

    fun requirePairing(state: NavState): NavState =
        if (state.stage == Stage.Splash && state.overlay == Overlay.None) state else NavState(stage = Stage.Pair)

    fun watch(state: NavState, source: StreamSource): NavState =
        if (!state.showsCards || state.overlay != Overlay.None) state else state.copy(overlay = Overlay.Stream(source))

    fun finishLeave(state: NavState): NavState =
        if (state.overlay is Overlay.Stream) state.copy(overlay = Overlay.None) else state

    fun sphere(state: NavState, tour: HomeChoice): NavState =
        if (state.stage != Stage.Tour || state.overlay != Overlay.None) state else state.copy(overlay = Overlay.Sphere(tour))

    fun sphereStatus(state: NavState, status: SphereStatus): NavState {
        val overlay = state.overlay as? Overlay.Sphere ?: return state
        // A failure stays a failure; it never turns into "darshan complete".
        if (overlay.status == SphereStatus.Failed && status == SphereStatus.Ended) return state
        return state.copy(overlay = overlay.copy(status = status))
    }

    fun handoff(state: NavState): NavState =
        if (state.stage != Stage.Tour || state.overlay != Overlay.None) state else state.copy(overlay = Overlay.Handoff)

    fun returnFromHandoff(state: NavState): NavState =
        if (state.overlay == Overlay.Handoff) state.copy(overlay = Overlay.None) else state

    fun openStates(state: NavState): NavState = NavState(stage = Stage.States)

    fun openTour(state: NavState): NavState = NavState(stage = Stage.Tour)

    fun openState(state: NavState, name: String): NavState =
        state.copy(stage = Stage.Live, stateName = name, index = 0, selectedId = null)

    fun backToStates(state: NavState, index: Int): NavState =
        state.copy(stage = Stage.States, index = index, selectedId = null)
}
