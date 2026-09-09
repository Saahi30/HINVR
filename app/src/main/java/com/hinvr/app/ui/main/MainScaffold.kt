package com.hinvr.app.ui.main

import androidx.compose.foundation.background
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

    Scaffold(
        containerColor = if (onPass) colors.duskDeep else colors.linen,
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
    Box(
        Modifier
            .fillMaxWidth()
            .background(if (sanctum) colors.duskDeep else Color.Transparent)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    16.dp,
                    RoundedCornerShape(36.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                )
                .clip(RoundedCornerShape(36.dp))
                .background(if (sanctum) colors.stone else colors.ivory.copy(alpha = 0.96f))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val idle = if (sanctum) colors.creamMuted else colors.inkMuted
                if (tab.route == Destinations.Pass) {
                    PassSeal(selected = selected, onClick = { onSelect(tab.route) })
                } else {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSelect(tab.route) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            tab.icon!!,
                            contentDescription = stringResource(tab.labelRes),
                            tint = if (selected) colors.gold else idle,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(tab.labelRes),
                            style = HinvrTypography.labelSmall.copy(letterSpacing = 0.2.sp),
                            color = if (selected) colors.gold else idle,
                        )
                    }
                }
            }
        }
    }
}
