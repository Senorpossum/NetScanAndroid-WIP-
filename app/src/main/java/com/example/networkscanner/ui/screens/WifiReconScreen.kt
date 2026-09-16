package com.example.networkscanner.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.networkscanner.data.repository.WifiScanAudit
import com.example.networkscanner.ui.components.WifiSpectrumChart
import com.example.networkscanner.ui.state.ScanUiState
import com.example.networkscanner.ui.viewmodel.WifiAuditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiReconScreen(
    viewModel: WifiAuditViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showChart by remember { mutableStateOf(false) }
    
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            viewModel.startWifiAudit()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wi-Fi Recon") },
                actions = {
                    IconButton(onClick = { showChart = !showChart }) {
                        Icon(
                            if (showChart) Icons.Default.List else Icons.Default.SignalCellularAlt,
                            contentDescription = "Toggle Chart"
                        )
                    }
                    Button(onClick = { permissionLauncher.launch(permissions.toTypedArray()) }) {
                        Text("Audit")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val currentState = state) {
                is ScanUiState.Idle -> {
                    Text("Press Audit to scan nearby networks.", modifier = Modifier.align(Alignment.Center))
                }
                is ScanUiState.Scanning -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text("Auditing Environment...", modifier = Modifier.align(Alignment.Center))
                }
                is ScanUiState.Success -> {
                    if (showChart) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("2.4 GHz Spectrum", style = MaterialTheme.typography.titleMedium)
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                WifiSpectrumChart(audits = currentState.data, is5Ghz = false)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("5 GHz Spectrum", style = MaterialTheme.typography.titleMedium)
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                WifiSpectrumChart(audits = currentState.data, is5Ghz = true)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = currentState.data,
                                key = { it.bssid } // Performance: Stable identity prevents recomposition
                            ) { audit ->
                                WifiAuditCard(audit)
                            }
                        }
                    }
                }
                is ScanUiState.Error -> {
                    Text("Error: ${currentState.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun WifiAuditCard(audit: WifiScanAudit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (audit.isPotentialRogue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (audit.isPotentialRogue) {
                Text(
                    text = "⚠️ POTENTIAL EVIL TWIN / ROGUE AP",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (audit.capabilities.contains("Open", ignoreCase = true) || audit.capabilities.contains("WEP", ignoreCase = true)) {
                 Text(
                    text = "⚠️ WEAK OR NO ENCRYPTION",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(audit.ssid, style = MaterialTheme.typography.titleMedium)
                    Text(audit.bssid, style = MaterialTheme.typography.bodySmall)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${audit.rssi} dBm", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
                    Badge {
                        val band = if (audit.frequency > 5000) "5G/6G" else "2.4G"
                        Text(band)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Capabilities: ${audit.capabilities}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
