package com.hinvr.app.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import com.hinvr.app.data.SessionRepository
import com.hinvr.app.data.SessionSnapshot

val LocalSessionRepository = staticCompositionLocalOf<SessionRepository> {
    error("SessionRepository not provided")
}

object Destinations {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Phone = "auth/phone"
    const val Otp = "auth/otp/{phone}"
    const val Setup = "auth/setup"
    const val Main = "main"
    const val Home = "home"
    const val Mandirs = "mandirs"
    const val MandirDetail = "mandirs/{id}"
    const val Live = "live"
    const val LivePlayer = "live/{id}"
    const val Vr = "vr"
    const val VrPlayer = "vr/{id}"
    const val Pass = "pass"
    const val PassHow = "pass/how"
    const val PassVisit = "pass/visit"
    const val Concierge = "concierge"
    const val Faq = "concierge/faq/{id}"
    const val Plans = "plans"
    const val PaySuccess = "plans/success"
    const val Profile = "profile"
    const val Legal = "profile/legal"
    const val Notifications = "notifications"
    const val Pooja = "pooja"
    const val Yatra = "yatra"

    fun otp(phone: String) = "auth/otp/${android.net.Uri.encode(phone)}"
    fun mandir(id: String) = "mandirs/$id"
    fun livePlayer(id: String) = "live/$id"
    fun vrPlayer(id: String) = "vr/$id"
    fun faq(id: String) = "concierge/faq/$id"
}

fun SessionSnapshot.startRoute(): String = when {
    isLoggedIn && profileComplete -> Destinations.Main
    isLoggedIn -> Destinations.Setup
    !hasOnboarded -> Destinations.Onboarding
    else -> Destinations.Phone
}
