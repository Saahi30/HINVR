package com.hinvr.app.ui.main

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hinvr.app.R
import com.hinvr.app.navigation.Destinations
import com.hinvr.app.ui.components.PassSeal
import com.hinvr.app.ui.concierge.ConciergeScreen
import com.hinvr.app.ui.home.HomeScreen
import com.hinvr.app.ui.mandirs.MandirsScreen
import com.hinvr.app.ui.motion.HinvrMotion
import com.hinvr.app.ui.pass.PassScreen
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

private data class TabSpec(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector?,
)

@Composable
fun MainScaffold(
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenRoute: (String) -> Unit,
) {
    val tabs = listOf(
        TabSpec(Destinations.Home, R.string.nav_home, Icons.Outlined.Home),
        TabSpec(Destinations.Mandirs, R.string.nav_mandirs, Icons.Outlined.AccountBalance),
        TabSpec(Destinations.Pass, R.string.nav_pass, null),
        TabSpec(Destinations.Concierge, R.string.nav_concierge, Icons.Outlined.ChatBubbleOutline),
    )
    val tabNav = rememberNavController()
    val current by tabNav.currentBackStackEntryAsState()
    val currentRoute = current?.destination?.route
    val colors = HinvrTheme.colors
    val onPass = currentRoute == Destinations.Pass
    val scaffoldColor by animateColorAsState(
        targetValue = if (onPass) colors.duskDeep else colors.linen,
        animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
        label = "main atmosphere",
    )

    Scaffold(
        containerColor = scaffoldColor,
        bottomBar = {
            HinvrTabDock(
                tabs = tabs,
                currentRoute = currentRoute,
                sanctum = onPass,
                onSelect = { route ->
                    tabNav.navigate(route) {
                        popUpTo(tabNav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = tabNav,
            startDestination = Destinations.Home,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            enterTransition = {
                val from = tabIndex(initialState.destination.route)
                val to = tabIndex(targetState.destination.route)
                val direction = if (to >= from) 1 else -1
                if (targetState.destination.route == Destinations.Pass) {
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
                        initialOffsetX = { width -> direction * width / 3 },
                    )
                }
            },
            exitTransition = {
                val from = tabIndex(initialState.destination.route)
                val to = tabIndex(targetState.destination.route)
                val direction = if (to >= from) 1 else -1
                fadeOut(
                    tween(HinvrMotion.Quick, easing = HinvrMotion.ExitEasing),
                ) + slideOutHorizontally(
                    animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.ExitEasing),
                    targetOffsetX = { width -> -direction * width / 8 },
                ) + scaleOut(
                    targetScale = 0.992f,
                    animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.ExitEasing),
                )
            },
            popEnterTransition = {
                fadeIn(tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing))
            },
            popExitTransition = {
                fadeOut(tween(HinvrMotion.Quick, easing = HinvrMotion.ExitEasing))
            },
        ) {
            composable(Destinations.Home) {
                HomeScreen(
                    onOpenProfile = onOpenProfile,
                    onOpenNotifications = onOpenNotifications,
                    onOpenPlans = onOpenPlans,
                    onOpenPass = {
                        tabNav.navigate(Destinations.Pass) { launchSingleTop = true }
                    },
                    onOpenConcierge = {
                        tabNav.navigate(Destinations.Concierge) { launchSingleTop = true }
                    },
                    onOpenRoute = onOpenRoute,
                )
            }
            composable(Destinations.Mandirs) {
                MandirsScreen(onOpenTemple = { onOpenRoute(Destinations.mandir(it)) })
            }
            composable(Destinations.Pass) {
                PassScreen(
                    onOpenPlans = onOpenPlans,
                    onOpenHow = { onOpenRoute(Destinations.PassHow) },
                    onPlanVisit = { onOpenRoute(Destinations.PassVisit) },
                )
            }
            composable(Destinations.Concierge) {
                ConciergeScreen(onOpenFaq = { onOpenRoute(Destinations.faq(it)) })
            }
        }
    }
}

@Composable
private fun HinvrTabDock(
    tabs: List<TabSpec>,
    currentRoute: String?,
    sanctum: Boolean,
    onSelect: (String) -> Unit,
) {
    val colors = HinvrTheme.colors
    val dockTop by animateColorAsState(
        targetValue = if (sanctum) colors.stoneRaised else Color(0xFFFFF8EF),
        animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
        label = "tab dock top",
    )
    val dockBottom by animateColorAsState(
        targetValue = if (sanctum) colors.stone else Color(0xFFF7EBDD),
        animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
        label = "tab dock bottom",
    )
    val dockOutside by animateColorAsState(
        targetValue = if (sanctum) colors.duskDeep else Color.Transparent,
        animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
        label = "tab dock outside",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .background(dockOutside)
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 11.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .shadow(
                    20.dp,
                    RoundedCornerShape(38.dp),
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .clip(RoundedCornerShape(38.dp))
                .border(
                    1.dp,
                    if (sanctum) colors.gold.copy(alpha = 0.2f) else colors.gold.copy(alpha = 0.14f),
                    RoundedCornerShape(38.dp),
                )
                .background(Brush.verticalGradient(listOf(dockTop, dockBottom)))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val idle = if (sanctum) colors.creamMuted else colors.inkMuted
                val selectedColor = if (sanctum) colors.gold else colors.saffron
                val itemScale by animateFloatAsState(
                    targetValue = if (selected) 1.08f else 0.96f,
                    animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
                    label = "${tab.route} selection",
                )
                if (tab.route == Destinations.Pass) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onSelect(tab.route) }
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PassSeal(
                            selected = selected,
                            onClick = { onSelect(tab.route) },
                            size = 45.dp,
                        )
                        Text(
                            stringResource(tab.labelRes),
                            style = HinvrTypography.labelSmall.copy(letterSpacing = 0.15.sp),
                            color = if (selected) selectedColor else idle,
                        )
                    }
                } else {
                    val itemBackground by animateColorAsState(
                        targetValue = if (selected) selectedColor.copy(alpha = 0.1f) else Color.Transparent,
                        animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
                        label = "${tab.route} background",
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .background(itemBackground)
                            .clickable { onSelect(tab.route) }
                            .padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            tab.icon!!,
                            contentDescription = stringResource(tab.labelRes),
                            tint = if (selected) selectedColor else idle,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            stringResource(tab.labelRes),
                            style = HinvrTypography.labelSmall.copy(letterSpacing = 0.15.sp),
                            color = if (selected) selectedColor else idle,
                        )
                    }
                }
            }
        }
    }
}

private fun tabIndex(route: String?): Int = when (route) {
    Destinations.Home -> 0
    Destinations.Mandirs -> 1
    Destinations.Pass -> 2
    Destinations.Concierge -> 3
    else -> 0
}
