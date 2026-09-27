package com.hinvr.app.navigation

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.hinvr.app.ui.auth.AccountScreen
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
    val session = LocalSessionRepository.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, session) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { session.syncRemote() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            OnboardingScreen { signIn ->
                replace(Destinations.account(signIn), Destinations.Onboarding)
            }
        }
        composable(
            Destinations.Phone,
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) { entry ->
            val signIn = entry.arguments?.getString("mode") == "signin"
            AccountScreen(
                startInSignIn = signIn,
                onAuthenticated = { profileComplete ->
                    if (profileComplete) {
                        nav.navigate(Destinations.Main) {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    } else {
                        replace(Destinations.Setup, Destinations.Phone)
                    }
                },
            )
        }
        composable(Destinations.Setup) {
            ProfileSetupScreen {
                nav.navigate(Destinations.Main) {
                    popUpTo(nav.graph.id) { inclusive = true }
                }
            }
        }
        composable(Destinations.Main) { entry ->
            val pendingTab by entry.savedStateHandle
                .getStateFlow(Destinations.MainTabKey, "")
                .collectAsStateWithLifecycle()
            MainScaffold(
                pendingTab = pendingTab,
                onPendingTabConsumed = {
                    entry.savedStateHandle[Destinations.MainTabKey] = ""
                },
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
                onPass = {
                    runCatching {
                        nav.getBackStackEntry(Destinations.Main)
                            .savedStateHandle[Destinations.MainTabKey] = Destinations.Pass
                    }
                    nav.popBackStack()
                },
                onAssist = { go(Destinations.passVisit(id)) },
                onPlans = { go(Destinations.Plans) },
                onConcierge = { go(Destinations.Concierge) },
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
        composable(
            Destinations.PassVisit,
            arguments = listOf(
                navArgument("mandir") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            PlanVisitScreen(
                onBack = { nav.popBackStack() },
                initialMandirId = entry.arguments?.getString("mandir").orEmpty(),
            )
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
            PlansScreen(onBack = { nav.popBackStack() })
        }
        composable(Destinations.Profile) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onLegal = { go(Destinations.Legal) },
                onPlans = { go(Destinations.Plans) },
                onSignedOut = {
                    nav.navigate(Destinations.account(signIn = true)) {
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
        route == Destinations.VrPlayer
