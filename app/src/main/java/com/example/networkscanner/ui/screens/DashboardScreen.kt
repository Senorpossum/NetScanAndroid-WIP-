package com.example.networkscanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.networkscanner.ui.viewmodel.DashboardViewModel
import com.example.networkscanner.ui.viewmodel.DashboardState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToLanScanner: () -> Unit,
    onNavigateToWifiAudit: () -> Unit,
    onNavigateToSnapshots: () -> Unit,
    onNavigateToAi: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isSentinelActive by viewModel.isSentinelActive.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NetSentinel Home") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NetworkInfoHeader(state)
            
            SentinelToggle(
                isActive = isSentinelActive,
                onToggle = { viewModel.toggleSentinel(it) }
            )

            QuickActions(onNavigateToLanScanner, onNavigateToWifiAudit, onNavigateToSnapshots)
            
            // Intelligence Summary Card
            SummaryStatisticsCard(state)

            // Navigation Card to AI Auditor
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToAi() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("NetSentinel AI Expert", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("On-device security consultant", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
fun SentinelToggle(isActive: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Network Sentinel", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (isActive) "Active background protection" else "Background monitoring disabled",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = isActive, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun NetworkInfoHeader(state: DashboardState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Active Network", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("SSID: ${state.currentSsid}", style = MaterialTheme.typography.bodyMedium)
            Text("Gateway: ${state.gateway}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Local IP: ${state.localIp}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("External IP: ${state.externalIp}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Subnet: ${state.subnetRange}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun QuickActions(
    onLan: () -> Unit,
    onWifi: () -> Unit,
    onSnapshots: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onLan, modifier = Modifier.weight(1f)) { Text("Scan Subnet", style = MaterialTheme.typography.labelSmall) }
        Button(onClick = onWifi, modifier = Modifier.weight(1f)) { Text("Audit Wi-Fi", style = MaterialTheme.typography.labelSmall) }
        Button(onClick = onSnapshots, modifier = Modifier.weight(1f)) { Text("History", style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
fun SummaryStatisticsCard(state: DashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Audit Summary", style = MaterialTheme.typography.titleMedium)
                Surface(
                    color = getHealthColor(state.healthScore).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Health: ${state.healthScore}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = getHealthColor(state.healthScore),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                StatItem("Hosts", state.totalHostsFound.toString(), Icons.Default.Devices)
                StatItem("Threats", state.vulnerablePortsExposed.toString(), Icons.Default.Security, color = if (state.vulnerablePortsExposed > 0) Color.Red else MaterialTheme.colorScheme.primary)
                StatItem("Rogue", state.rogueApWarnings.toString(), Icons.Default.Warning, color = if (state.rogueApWarnings > 0) Color.Red else MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = MaterialTheme.colorScheme.primary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

fun getHealthColor(score: Int): Color {
    return when {
        score >= 90 -> Color(0xFF4CAF50)
        score >= 70 -> Color(0xFFFFC107)
        else -> Color.Red
    }
}
