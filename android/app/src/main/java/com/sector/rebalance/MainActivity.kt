package com.sector.rebalance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sector.rebalance.ui.action.RebalanceActionScreen
import com.sector.rebalance.ui.analytics.AnalyticsScreen
import com.sector.rebalance.ui.dashboard.DashboardScreen
import com.sector.rebalance.ui.dashboard.DashboardViewModel
import com.sector.rebalance.ui.settings.SettingsScreen
import com.sector.rebalance.ui.theme.BackgroundDark
import com.sector.rebalance.ui.theme.PrimaryTeal
import com.sector.rebalance.ui.theme.SectorRebalanceTheme
import com.sector.rebalance.ui.theme.SurfaceDark
import com.sector.rebalance.ui.theme.TextPrimary
import com.sector.rebalance.ui.theme.TextSecondary

sealed class NavTab(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : NavTab("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Rebalance : NavTab("rebalance", "Rebalance", Icons.Default.Autorenew)
    object Analytics : NavTab("analytics", "Analytics", Icons.Default.AutoGraph)
    object Settings : NavTab("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SectorApp
        val viewModel = DashboardViewModel(app.repository)

        setContent {
            SectorRebalanceTheme {
                val navController = rememberNavController()
                val tabs = listOf(NavTab.Dashboard, NavTab.Rebalance, NavTab.Analytics, NavTab.Settings)
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: NavTab.Dashboard.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BackgroundDark,
                    bottomBar = {
                        NavigationBar(
                            containerColor = SurfaceDark,
                            contentColor = TextPrimary
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != tab.route) {
                                            navController.navigate(tab.route) {
                                                popUpTo(NavTab.Dashboard.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                                    label = { Text(text = tab.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = BackgroundDark,
                                        selectedTextColor = PrimaryTeal,
                                        indicatorColor = PrimaryTeal,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavTab.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavTab.Dashboard.route) {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToRebalance = {
                                    navController.navigate(NavTab.Rebalance.route)
                                }
                            )
                        }
                        composable(NavTab.Rebalance.route) {
                            RebalanceActionScreen(viewModel = viewModel)
                        }
                        composable(NavTab.Analytics.route) {
                            AnalyticsScreen(repository = app.repository)
                        }
                        composable(NavTab.Settings.route) {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
