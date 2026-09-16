package com.example.networkscanner.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.networkscanner.ui.screens.*
import com.example.networkscanner.ui.viewmodel.*

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = TopLevelRoute.HOME.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(TopLevelRoute.HOME.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToLanScanner = { navController.navigate(TopLevelRoute.DISCOVERY.route) },
                onNavigateToWifiAudit = { navController.navigate("wifi_recon") },
                onNavigateToSnapshots = { navController.navigate(TopLevelRoute.REPORTS.route) },
                onNavigateToAi = { navController.navigate(TopLevelRoute.AI_AUDITOR.route) }
            )
        }
        
        composable(TopLevelRoute.DISCOVERY.route) {
            val viewModel: LanScannerViewModel = hiltViewModel()
            LanDevicesListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("wifi_recon") {
            val viewModel: WifiAuditViewModel = hiltViewModel()
            WifiReconScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(TopLevelRoute.DIAGNOSTICS.route) {
            val viewModel: NetworkHealthViewModel = hiltViewModel()
            val idleMetrics by viewModel.idleMetrics.collectAsState()
            val bufferbloatResult by viewModel.bufferbloatResult.collectAsState()
            val isTesting by viewModel.isTesting.collectAsState()
            
            NetworkHealthScreen(
                viewModel = viewModel,
                idleMetrics = idleMetrics,
                bufferbloatResult = bufferbloatResult,
                isTesting = isTesting,
                onRunLoadTest = { viewModel.runLoadTest() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("signal_tracker") {
            val viewModel: SignalTrackerViewModel = hiltViewModel()
            SignalTrackerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${TopLevelRoute.AI_AUDITOR.route}?snapshotId={snapshotId}",
            arguments = listOf(navArgument("snapshotId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val snapshotId = backStackEntry.arguments?.getLong("snapshotId") ?: -1L
            val viewModel: LlmViewModel = hiltViewModel()
            
            LaunchedEffect(snapshotId) {
                if (snapshotId != -1L) {
                    viewModel.loadAndAnalyzeReport(snapshotId)
                }
            }
            
            LlmScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(TopLevelRoute.REPORTS.route) {
            val viewModel: ReportsViewModel = hiltViewModel()
            ReportsScreen(
                viewModel = viewModel,
                onAiAudit = { snapshotId ->
                    navController.navigate("${TopLevelRoute.AI_AUDITOR.route}?snapshotId=$snapshotId")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
