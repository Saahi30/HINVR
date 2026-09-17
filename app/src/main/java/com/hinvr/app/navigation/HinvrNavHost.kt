package com.hinvr.app.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hinvr.app.ui.auth.OtpScreen
import com.hinvr.app.ui.auth.PhoneLoginScreen
import com.hinvr.app.ui.auth.ProfileSetupScreen
import com.hinvr.app.ui.concierge.ConciergeScreen
import com.hinvr.app.ui.concierge.FaqArticleScreen
import com.hinvr.app.ui.desk.PoojaScreen
import com.hinvr.app.ui.desk.YatraScreen
import com.hinvr.app.ui.live.LiveListScreen
import com.hinvr.app.ui.live.LivePlayerScreen
import com.hinvr.app.ui.main.MainScaffold
import com.hinvr.app.ui.motion.HinvrMotion
import com.hinvr.app.ui.mandirs.TempleDetailScreen
import com.hinvr.app.ui.onboarding.OnboardingScreen
import com.hinvr.app.ui.pass.HowPassWorksScreen
import com.hinvr.app.ui.pass.PlanVisitScreen
import com.hinvr.app.ui.plans.PaySuccessScreen
import com.hinvr.app.ui.plans.PlansScreen
import com.hinvr.app.ui.profile.LegalScreen
import com.hinvr.app.ui.profile.NotificationsScreen
import com.hinvr.app.ui.profile.ProfileScreen
import com.hinvr.app.ui.splash.SplashScreen
import com.hinvr.app.ui.vr.VrListScreen
import com.hinvr.app.ui.vr.VrPlayerScreen

@Composable
fun HinvrNavHost() {
    val nav = rememberNavController()

    fun go(route: String) = nav.navigate(route)

    fun replace(route: String, pop: String) {
        nav.navigate(route) {
            popUpTo(pop) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = nav,
        startDestination = Destinations.Splash,
        enterTransition = {
            val immersive = isImmersiveRoute(targetState.destination.route)
            if (immersive) {
                fadeIn(
                    tween(HinvrMotion.Immersive, easing = HinvrMotion.EnterEasing),
                ) + scaleIn(
                    initialScale = 1.035f,
                    animationSpec = tween(HinvrMotion.Immersive, easing = HinvrMotion.EnterEasing),
                )
            } else {
                fadeIn(
                    tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
                ) + slideInHorizontally(
                    animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
                    initialOffsetX = { width -> width / 3 },
                )
            }
        },
        exitTransition = {
            fadeOut(
                tween(HinvrMotion.Quick, easing = HinvrMotion.ExitEasing),
            ) + slideOutHorizontally(
                animationSpec = tween(HinvrMotion.Quick, easing = HinvrMotion.ExitEasing),
                targetOffsetX = { width -> -width / 9 },
            )
        },
        popEnterTransition = {
            fadeIn(
                tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
            ) + slideInHorizontally(
                animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
                initialOffsetX = { width -> -width / 3 },
            )
        },
        popExitTransition = {
            fadeOut(
                tween(HinvrMotion.Quick, easing = HinvrMotion.ExitEasing),
            ) + slideOutHorizontally(
                animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
                targetOffsetX = { width -> width / 3 },
            ) + scaleOut(
                targetScale = 0.99f,
                animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
            )
        },
    ) {
        composable(Destinations.Splash) {
            SplashScreen { route ->
                nav.navigate(route) {
                    popUpTo(Destinations.Splash) { inclusive = true }
                }
            }
        }
        composable(Destinations.Onboarding) {
            OnboardingScreen {
                replace(Destinations.Phone, Destinations.Onboarding)
            }
        }
        composable(Destinations.Phone) {
            PhoneLoginScreen(
                onOtpSent = { phone -> go(Destinations.otp(phone)) },
            )
        }
        composable(
            Destinations.Otp,
            arguments = listOf(navArgument("phone") { type = NavType.StringType }),
        ) { entry ->
            val phone = entry.arguments?.getString("phone").orEmpty()
            OtpScreen(
                phone = phone,
                onVerified = { profileComplete ->
                    if (profileComplete) {
                        nav.navigate(Destinations.Main) {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    } else {
                        replace(Destinations.Setup, Destinations.Phone)
                    }
                },
                onChangeNumber = { nav.popBackStack() },
            )
        }
        composable(Destinations.Setup) {
            ProfileSetupScreen {
                nav.navigate(Destinations.Main) {
                    popUpTo(nav.graph.id) { inclusive = true }
                }
            }
        }
        composable(Destinations.Main) {
            MainScaffold(
                onOpenProfile = { go(Destinations.Profile) },
                onOpenNotifications = { go(Destinations.Notifications) },
                onOpenPlans = { go(Destinations.Plans) },
                onOpenRoute = { route -> go(route) },
            )
        }
        composable(
            Destinations.MandirDetail,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            TempleDetailScreen(
                id = id,
                onBack = { nav.popBackStack() },
                onLive = { go(Destinations.livePlayer(id)) },
                onVr = { go(Destinations.vrPlayer(id)) },
                onPass = { go(Destinations.Plans) },
                onAssist = { go(Destinations.Concierge) },
            )
        }
        composable(Destinations.Live) {
            LiveListScreen(
                onBack = { nav.popBackStack() },
                onOpenPlayer = { go(Destinations.livePlayer(it)) },
            )
        }
        composable(
            Destinations.LivePlayer,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            LivePlayerScreen(
                id = entry.arguments?.getString("id").orEmpty(),
                onBack = { nav.popBackStack() },
                onPlans = { go(Destinations.Plans) },
            )
        }
        composable(Destinations.Vr) {
            VrListScreen(
                onBack = { nav.popBackStack() },
                onOpenPlayer = { go(Destinations.vrPlayer(it)) },
                onPlans = { go(Destinations.Plans) },
            )
        }
        composable(
            Destinations.VrPlayer,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            VrPlayerScreen(
                id = entry.arguments?.getString("id").orEmpty(),
                onBack = { nav.popBackStack() },
                onPlans = { go(Destinations.Plans) },
            )
        }
        composable(Destinations.Concierge) {
            ConciergeScreen(
                onOpenFaq = { go(Destinations.faq(it)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Destinations.PassHow) {
            HowPassWorksScreen(onBack = { nav.popBackStack() })
        }
        composable(Destinations.PassVisit) {
            PlanVisitScreen(onBack = { nav.popBackStack() })
        }
        composable(
            Destinations.Faq,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            FaqArticleScreen(
                id = entry.arguments?.getString("id").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
        composable(Destinations.Plans) {
            PlansScreen(
                onBack = { nav.popBackStack() },
                onMockPay = { replace(Destinations.PaySuccess, Destinations.Plans) },
            )
        }
        composable(Destinations.PaySuccess) {
            PaySuccessScreen {
                nav.navigate(Destinations.Main) {
                    popUpTo(nav.graph.id) { inclusive = true }
                }
            }
        }
        composable(Destinations.Profile) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onLegal = { go(Destinations.Legal) },
                onPlans = { go(Destinations.Plans) },
                onSignedOut = {
                    nav.navigate(Destinations.Phone) {
                        popUpTo(nav.graph.id) { inclusive = true }
                    }
                },
            )
        }
        composable(Destinations.Legal) {
            LegalScreen(onBack = { nav.popBackStack() })
        }
        composable(Destinations.Notifications) {
            NotificationsScreen(onBack = { nav.popBackStack() })
        }
        composable(Destinations.Pooja) {
            PoojaScreen(
                onBack = { nav.popBackStack() },
                onConcierge = { go(Destinations.Concierge) },
            )
        }
        composable(Destinations.Yatra) {
            YatraScreen(
                onBack = { nav.popBackStack() },
                onConcierge = { go(Destinations.Concierge) },
            )
        }
    }
}

private fun isImmersiveRoute(route: String?): Boolean =
    route == Destinations.LivePlayer ||
        route == Destinations.VrPlayer ||
        route == Destinations.PaySuccess
