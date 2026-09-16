package com.example.networkscanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.networkscanner.domain.service.PortResult
import com.example.networkscanner.domain.service.RiskSeverity
import com.example.networkscanner.domain.model.DiscoveredHost
import com.example.networkscanner.ui.components.HostActionBottomSheet
import com.example.networkscanner.ui.components.NetworkTopologyGraph
import com.example.networkscanner.ui.state.ScanUiState
import com.example.networkscanner.ui.viewmodel.HostWithPorts
import com.example.networkscanner.ui.viewmodel.LanScannerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanDevicesListScreen(
    viewModel: LanScannerViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showTopology by remember { mutableStateOf(false) }
    var selectedHostForTools by remember { mutableStateOf<HostWithPorts?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("LAN Devices") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.saveCurrentSnapshot()
                        scope.launch {
                            snackbarHostState.showSnackbar("Snapshot saved to Reports")
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save Snapshot")
                    }
                    IconButton(onClick = { showTopology = !showTopology }) {
                        Icon(
                            if (showTopology) Icons.Default.List else Icons.Default.Hub,
                            contentDescription = "Toggle Topology"
                        )
                    }
                    IconButton(onClick = { viewModel.scanNetwork() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val currentState = state) {
                is ScanUiState.Idle -> {
                    Text("Press Scan to discover network devices.", modifier = Modifier.align(Alignment.Center))
                }
                is ScanUiState.Scanning -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sweeping /24 Subnet...")
                    }
                }
                is ScanUiState.Success -> {
                    if (showTopology) {
                        NetworkTopologyGraph(
                            hosts = currentState.data,
                            onNodeTap = { selectedHostForTools = it }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = currentState.data,
                                key = { it.host.ipAddress }
                            ) { hostWithPorts ->
                                HostCard(
                                    hostWithPorts = hostWithPorts,
                                    onScanPorts = { viewModel.scanPortsForHost(hostWithPorts) }
                                )
                            }
                        }
                    }
                }
                is ScanUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${currentState.message}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.scanNetwork() }) { Text("Retry") }
                    }
                }
            }

            selectedHostForTools?.let { hostWithPorts ->
                HostActionBottomSheet(
                    host = DiscoveredHost(
                        ipAddress = hostWithPorts.host.ipAddress,
                        macAddress = hostWithPorts.host.macAddress,
                        vendorOui = hostWithPorts.host.vendor,
                        hostname = hostWithPorts.host.hostname,
                        openPorts = hostWithPorts.openPorts.map { it.port },
                        isNewDevice = false
                    ),
                    onDismissRequest = { selectedHostForTools = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostCard(
    hostWithPorts: HostWithPorts,
    onScanPorts: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(hostWithPorts.host.ipAddress, style = MaterialTheme.typography.titleMedium)
                    Text(hostWithPorts.host.hostname ?: "Unknown Host", style = MaterialTheme.typography.bodyMedium)
                    val details = buildString {
                        if (hostWithPorts.host.macAddress == null || hostWithPorts.host.macAddress == "00:00:00:00:00:00") {
                            append("MAC Restricted (Android 10+)")
                        } else {
                            append(hostWithPorts.host.macAddress)
                        }
                        hostWithPorts.host.vendor?.let { append(" • $it") }
                    }
                    Text(details, style = MaterialTheme.typography.bodySmall)
                }
                if (hostWithPorts.openPorts.isNotEmpty()) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text("${hostWithPorts.openPorts.size} Open")
                    }
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Port Scan Results:", style = MaterialTheme.typography.labelLarge)
                        TextButton(onClick = onScanPorts) {
                            Text("Scan Ports")
                        }
                    }
                    
                    if (hostWithPorts.openPorts.isEmpty()) {
                        Text("No open critical ports found or not scanned.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            hostWithPorts.openPorts.forEach { portResult ->
                                PortBadge(portResult)
                            }
                        }
                    }
                    
                    if (hostWithPorts.securityAdvice.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Remediation Advice:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                        hostWithPorts.securityAdvice.forEach { advice ->
                            Text(
                                text = "• $advice",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PortBadge(portResult: PortResult) {
    val color = when (portResult.riskSeverity) {
        RiskSeverity.HIGH -> Color.Red
        RiskSeverity.MED -> Color(0xFFFFA500) // Amber
        RiskSeverity.LOW -> Color.Blue
        RiskSeverity.UNKNOWN -> Color.Gray
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = "${portResult.port}/${portResult.serviceName}",
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
