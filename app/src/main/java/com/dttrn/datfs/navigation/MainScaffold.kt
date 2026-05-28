package com.dttrn.datfs.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dttrn.datfs.core.data.datastore.SettingsDataStore

/**
 * Các tab chính của Bottom Navigation Bar.
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        label = "Trang chủ",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    BottomNavItem(
        route = Screen.Search.route,
        label = "Tìm kiếm",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
    ),
    BottomNavItem(
        route = Screen.Statistics.route,
        label = "Thống kê",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        label = "Cài đặt",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
)

/**
 * Danh sách các route mà Bottom Nav nên ẩn đi (ví dụ: màn học, màn chỉnh sửa...).
 */
private val routesWithoutBottomBar = setOf(
    Screen.Splash.route,
    Screen.Onboarding.route,
    Screen.StudyModePicker.route.substringBefore("{"),
    Screen.StudySession.route.substringBefore("{"),
    Screen.StudyResult.route.substringBefore("{"),
    Screen.CreateEditDeck.route.substringBefore("?"),
    Screen.CardEditor.route.substringBefore("{"),
    Screen.DeckDetail.route.substringBefore("{"),
    Screen.ImportExport.route,
    Screen.Backup.route,
    Screen.ExamConfig.route.substringBefore("?"),
    Screen.ExamSession.route.substringBefore("{"),
    Screen.ExamResult.route.substringBefore("{"),
)

@Composable
fun MainScaffold(
    settingsDataStore: SettingsDataStore? = null,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Kiểm tra có nên hiển thị bottom nav không
    val showBottomBar = currentDestination?.route?.let { route ->
        routesWithoutBottomBar.none { prefix -> route.startsWith(prefix) }
    } ?: false  // false by default to hide during splash

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(
                    animationSpec = tween(200),
                    initialOffsetY = { it },
                ),
                exit = slideOutVertically(
                    animationSpec = tween(200),
                    targetOffsetY = { it },
                ),
            ) {
                FlashMindBottomNav(
                    items = bottomNavItems,
                    currentDestination = currentDestination,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            // Pop up to start destination để tránh stack tích tụ
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavGraph(
            modifier = Modifier
                .fillMaxSize(),
            navController = navController,
            settingsDataStore = settingsDataStore,
        )
    }
}

@Composable
private fun FlashMindBottomNav(
    items: List<BottomNavItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onItemClick: (BottomNavItem) -> Unit,
) {
    NavigationBar {
        items.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                alwaysShowLabel = true,
            )
        }
    }
}
