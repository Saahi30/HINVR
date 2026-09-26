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
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
import com.hinvr.app.ui.components.LocalDockClearance
import com.hinvr.app.ui.components.PassSeal
import com.hinvr.app.ui.concierge.ConciergeScreen
import com.hinvr.app.ui.home.HomeScreen
import com.hinvr.app.ui.mandirs.MandirsScreen
import com.hinvr.app.ui.motion.HinvrMotion
import com.hinvr.app.ui.pass.PassScreen
import com.hinvr.app.ui.theme.HinvrPillRadius
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

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
    val onPass = currentRoute == Destinations.Pass
    val hazeState = rememberHazeState()
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
                hazeState = hazeState,
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
                modifier = Modifier.hazeSource(hazeState),
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
                        onPlanVisit = { onOpenRoute(Destinations.PassVisit) },
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
private val DockGlass = Color(0xFF17100B)
private val DockText = Color(0xFFF7EFE4)

/**
 * A pill of smoked glass floating over the page, which blurs through it. The
 * active tab sits in a lighter inner pill with its icon lit in its own color;
 * the Pass seal rides in the bar as a glossy orb.
 */
@Composable
private fun HinvrTabDock(
    tabs: List<TabSpec>,
    currentRoute: String?,
    hazeState: HazeState,
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
                .shadow(
                    20.dp,
                    shape,
                    ambientColor = Color.Black.copy(alpha = 0.28f),
                    spotColor = Color.Black.copy(alpha = 0.34f),
                )
                .clip(shape)
                .hazeEffect(state = hazeState) {
                    blurRadius = 24.dp
                    noiseFactor = 0.05f
                    tints = listOf(HazeTint(DockGlass.copy(alpha = 0.84f)))
                    fallbackTint = HazeTint(DockGlass.copy(alpha = 0.94f))
                }
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f)),
                    ),
                    shape,
                )
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

/** Each tab lights its icon in its own glaze, like the enamel on a temple bell. */
private fun tabGlaze(route: String): Brush = Brush.verticalGradient(
    when (route) {
        Destinations.Home -> listOf(Color(0xFFFFC46B), Color(0xFFEE5F27))
        Destinations.Mandirs -> listOf(Color(0xFFFFE391), Color(0xFFD49A18))
        else -> listOf(Color(0xFFD2B4FF), Color(0xFF7C4DEB))
    },
)

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
    val glaze = remember(tab.route) { tabGlaze(tab.route) }
    val shape = RoundedCornerShape(HinvrPillRadius)

    Row(
        modifier = Modifier
            .height(DockHeight - 12.dp)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .clip(shape)
            .background(Color.White.copy(alpha = 0.13f * lit))
            .border(1.dp, Color.White.copy(alpha = 0.07f * lit), shape)
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
            if (on) {
                Icon(
                    tab.selectedIcon ?: icon,
                    contentDescription = if (showLabel) null else label,
                    tint = Color.White,
                    modifier = Modifier
                        .size(23.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(glaze, blendMode = BlendMode.SrcIn)
                        },
                )
            } else {
                Icon(
                    icon,
                    contentDescription = if (showLabel) null else label,
                    tint = DockText.copy(alpha = 0.86f),
                    modifier = Modifier.size(23.dp),
                )
            }
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
                color = DockText.copy(alpha = if (selected) 1f else 0.86f),
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
