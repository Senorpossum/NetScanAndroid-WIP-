package com.example.networkscanner.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.networkscanner.ui.components.PermissionCoordinator

@Composable
fun RootScaffold() {
    // Intercepts the UI root until capabilities are granted
    PermissionCoordinator {
        
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val routes = remember { TopLevelRoute.values() }

        // Adaptive Scaffold: Automatically uses a BottomNavigationBar on phones
        // and transitions to a NavigationRail on tablets/landscape.
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                routes.forEach { route ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == route.route } == true
                    item(
                        icon = { Icon(imageVector = route.icon, contentDescription = route.title) },
                        label = { Text(route.title) },
                        selected = isSelected,
                        onClick = {
                            // Standard robust navigation pattern for bottom bars
                            navController.navigate(route.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) {
            AppNavigation(navController = navController)
        }
    }
}
