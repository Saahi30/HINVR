package com.hinvr.app.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hinvr.app.R
import com.hinvr.app.navigation.Destinations
import com.hinvr.app.ui.components.ClayGlassState
import com.hinvr.app.ui.components.LocalDockClearance
import com.hinvr.app.ui.components.PassSeal
import com.hinvr.app.ui.components.clayGlass
import com.hinvr.app.ui.components.clayGlassSource
import com.hinvr.app.ui.concierge.ConciergeScreen
import com.hinvr.app.ui.home.HomeScreen
import com.hinvr.app.ui.mandirs.MandirsScreen
import com.hinvr.app.ui.motion.HinvrMotion
import com.hinvr.app.ui.pass.PassScreen
import com.hinvr.app.ui.theme.HinvrPillRadius
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import com.hinvr.app.ui.theme.ServiceClay

private data class TabSpec(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector?,
    val selectedIcon: ImageVector? = null,
)

@Composable
fun MainScaffold(
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenRoute: (String) -> Unit,
    pendingTab: String = "",
    onPendingTabConsumed: () -> Unit = {},
) {
    val tabs = listOf(
        TabSpec(Destinations.Home, R.string.nav_home, Icons.Outlined.Home, Icons.Rounded.Home),
        TabSpec(Destinations.Mandirs, R.string.nav_mandirs, Icons.Outlined.AccountBalance, Icons.Rounded.AccountBalance),
        TabSpec(Destinations.Pass, R.string.nav_pass, null),
        TabSpec(Destinations.Concierge, R.string.nav_concierge, Icons.Outlined.ChatBubbleOutline, Icons.Rounded.ChatBubble),
    )
    val tabNav = rememberNavController()
    val current by tabNav.currentBackStackEntryAsState()
    val currentRoute = current?.destination?.route
    val colors = HinvrTheme.colors
    val glass = remember { ClayGlassState() }
    val onPass = currentRoute == Destinations.Pass
    LaunchedEffect(pendingTab) {
        if (pendingTab.isBlank()) return@LaunchedEffect
        tabNav.navigateToTab(pendingTab)
        onPendingTabConsumed()
    }
    val scaffoldColor by animateColorAsState(
        targetValue = if (onPass) colors.duskDeep else colors.linen,
        animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
        label = "main atmosphere",
    )

    Scaffold(
        containerColor = scaffoldColor,
        bottomBar = {
            HinvrTabDock(
                glass = glass,
                tabs = tabs,
                currentRoute = currentRoute,
                onSelect = { route ->
                    if (route == currentRoute) return@HinvrTabDock
                    tabNav.navigateToTab(route)
                },
            )
        },
    ) { padding ->
        CompositionLocalProvider(LocalDockClearance provides padding.calculateBottomPadding()) {
            NavHost(
                navController = tabNav,
                startDestination = Destinations.Home,
                modifier = Modifier.clayGlassSource(glass),
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
                        onOpenPass = { tabNav.navigateToTab(Destinations.Pass) },
                        onOpenConcierge = { tabNav.navigateToTab(Destinations.Concierge) },
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
                        onPlanVisit = { onOpenRoute(Destinations.passVisit()) },
                    )
                }
                composable(Destinations.Concierge) {
                    ConciergeScreen(onOpenFaq = { onOpenRoute(Destinations.faq(it)) })
                }
            }
        }
    }
}

private val DockHeight = 62.dp
private val DockOrbSize = 48.dp

/**
 * Clay-tinted glass. The blur stays gentle and the tint is the service-card clay,
 * so the bar reads as smoked parchment rather than grey glass.
 */
@Composable
private fun HinvrTabDock(
    glass: ClayGlassState,
    tabs: List<TabSpec>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    val shape = RoundedCornerShape(HinvrPillRadius)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp),
    ) {
        // Every tab keeps its label when there is room; on narrow phones only the active one does.
        val roomy = maxWidth >= 376.dp
        Row(
            Modifier
                .fillMaxWidth()
                .height(DockHeight)
                .clip(shape)
                .clayGlass(
                    state = glass,
                    blurRadius = 16.dp,
                    tint = ServiceClay.copy(alpha = 0.9f),
                )
                .border(1.dp, Color(0xFFF3EEE2).copy(alpha = 0.34f), shape)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (roomy) Arrangement.SpaceBetween else Arrangement.SpaceAround,
        ) {
            tabs.forEach { tab ->
                val selected = tab.route == currentRoute
                if (tab.icon == null) {
                    val label = stringResource(tab.labelRes)
                    PassSeal(
                        selected = selected,
                        onClick = { onSelect(tab.route) },
                        size = DockOrbSize,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = label
                                role = Role.Tab
                                this.selected = selected
                            },
                    )
                } else {
                    DockTab(
                        tab = tab,
                        icon = tab.icon,
                        selected = selected,
                        showLabel = roomy || selected,
                        onClick = { onSelect(tab.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DockTab(
    tab: TabSpec,
    icon: ImageVector,
    selected: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(tab.labelRes)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(HinvrMotion.Quick, easing = HinvrMotion.EnterEasing),
        label = "${tab.route} press",
    )
    val lit by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
        label = "${tab.route} lit",
    )
    val colors = HinvrTheme.colors
    val shape = RoundedCornerShape(HinvrPillRadius)

    Row(
        modifier = Modifier
            .height(DockHeight - 12.dp)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .clip(shape)
            .border(1.dp, colors.gold.copy(alpha = lit), shape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null,
            )
            .animateContentSize(tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing))
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crossfade(
            targetState = selected,
            animationSpec = tween(HinvrMotion.Quick),
            label = "${tab.route} icon",
        ) { on ->
            Icon(
                if (on) tab.selectedIcon ?: icon else icon,
                contentDescription = if (showLabel) null else label,
                tint = if (on) colors.gold else colors.cream.copy(alpha = 0.72f),
                modifier = Modifier.size(23.dp),
            )
        }
        AnimatedVisibility(
            visible = showLabel,
            enter = fadeIn(tween(HinvrMotion.Standard)) + expandHorizontally(tween(HinvrMotion.Standard)),
            exit = fadeOut(tween(HinvrMotion.Quick)) + shrinkHorizontally(tween(HinvrMotion.Standard)),
        ) {
            Text(
                label,
                style = HinvrTypography.labelLarge.copy(
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = 0.1.sp,
                ),
                color = if (selected) colors.cream else colors.cream.copy(alpha = 0.72f),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

private fun NavController.navigateToTab(route: String) {
    val startId = graph.findStartDestination().id
    if (route == Destinations.Home) {
        // launchSingleTop plus popUpTo(start) is a no-op when Home is already
        // the root under the current tab, so the Home button never leaves Pass.
        if (!popBackStack(startId, inclusive = false)) {
            navigate(Destinations.Home) {
                popUpTo(startId) { inclusive = true }
                launchSingleTop = true
            }
        }
        return
    }
    navigate(route) {
        popUpTo(startId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun tabIndex(route: String?): Int = when (route) {
    Destinations.Home -> 0
    Destinations.Mandirs -> 1
    Destinations.Pass -> 2
    Destinations.Concierge -> 3
    else -> 0
}
