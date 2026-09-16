package com.example.networkscanner.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines the top-level routes for the application shell.
 * Reduced to 5 items for optimal mobile BottomBar real estate.
 */
enum class TopLevelRoute(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    ),
    DISCOVERY(
        route = "discovery",
        title = "Scan",
        icon = Icons.Default.Search
    ),
    DIAGNOSTICS(
        route = "diagnostics",
        title = "Health",
        icon = Icons.Default.Speed
    ),
    AI_AUDITOR(
        route = "ai_auditor",
        title = "AI",
        icon = Icons.Default.AutoAwesome
    ),
    REPORTS(
        route = "reports",
        title = "History",
        icon = Icons.Default.Description
    )
}
