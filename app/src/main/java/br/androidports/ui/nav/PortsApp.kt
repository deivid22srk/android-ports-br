package br.androidports.ui.nav

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.androidports.di.AppContainer
import br.androidports.ui.components.GlassSurface
import br.androidports.ui.explore.ExploreScreen
import br.androidports.ui.feed.FeedScreen
import br.androidports.ui.saved.SavedScreen
import br.androidports.ui.settings.SettingsScreen
import br.androidports.ui.theme.LocalHapticsEnabled
import br.androidports.ui.theme.PortsTheme

private data class BottomItem(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val bottomItems = listOf(
    BottomItem(FeedRoute, "Feed", Icons.Outlined.Home, Icons.Filled.Home),
    BottomItem(ExploreRoute, "Explorar", Icons.Outlined.Explore, Icons.Filled.Explore),
    BottomItem(SavedRoute, "Salvos", Icons.Outlined.Bookmark, Icons.Filled.Bookmark),
    BottomItem(SettingsRoute, "Ajustes", Icons.Outlined.Settings, Icons.Filled.Settings),
)

@Composable
fun PortsApp(container: AppContainer, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val colors = PortsTheme.colors

    fun navigateTo(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier.fillMaxSize().background(colors.background)) {
        NavHost(
            navController = navController,
            startDestination = FeedRoute,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable<FeedRoute> {
                FeedScreen(
                    repository = container.repository,
                    feedFilter = container.feedFilter,
                )
            }
            composable<ExploreRoute> {
                ExploreScreen(
                    repository = container.repository,
                    onOpenCategory = { category ->
                        container.feedFilter.select(category)
                        navigateTo(FeedRoute)
                    },
                )
            }
            composable<SavedRoute> {
                SavedScreen(
                    repository = container.repository,
                    onExplore = { navigateTo(ExploreRoute) },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(container = container)
            }
        }

        PortsBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            currentRoute = currentRoute,
            onClick = { item ->
                if (currentRoute != item.route::class.qualifiedName) {
                    navigateTo(item.route)
                }
            },
        )
    }
}

@Composable
private fun PortsBottomBar(
    currentRoute: String?,
    onClick: (BottomItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PortsTheme.colors
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            color = colors.glassStrong,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bottomItems.forEach { item ->
                    val active = currentRoute == item.route::class.qualifiedName
                    val dotScale by animateFloatAsState(
                        targetValue = if (active) 1f else 0f,
                        animationSpec = PortsTheme.motion.spring,
                        label = "navDot_${item.label}",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .clickable {
                                if (hapticsEnabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onClick(item)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (active) item.selectedIcon else item.icon,
                                contentDescription = item.label,
                                tint = if (active) colors.textPrimary else colors.textTertiary,
                                modifier = Modifier.size(23.dp),
                            )
                            Box(
                                Modifier
                                    .offset(x = 15.dp, y = (-13).dp)
                                    .size(6.dp)
                                    .graphicsLayer {
                                        scaleX = dotScale
                                        scaleY = dotScale
                                        alpha = dotScale
                                    }
                                    .clip(CircleShape)
                                    .background(colors.accent),
                            )
                        }
                    }
                }
            }
        }
    }
}
