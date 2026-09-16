package com.example.networkscanner.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.networkscanner.domain.service.DownloadState
import com.example.networkscanner.ui.viewmodel.LlmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmScreen(
    viewModel: LlmViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showReportPicker by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importModel(it) }
    }

    if (showReportPicker) {
        AlertDialog(
            onDismissRequest = { showReportPicker = false },
            title = { Text("Select Network Snapshot") },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (state.savedSnapshots.isEmpty()) {
                        Text("No saved reports found.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(state.savedSnapshots.size) { index ->
                                val snapshotWithHosts = state.savedSnapshots[index]
                                TextButton(
                                    onClick = {
                                        viewModel.loadAndAnalyzeReport(snapshotWithHosts.snapshot.id)
                                        showReportPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                        Text(snapshotWithHosts.snapshot.ssid, style = MaterialTheme.typography.labelLarge)
                                        Text("${snapshotWithHosts.hosts.size} devices indexed", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReportPicker = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NetSentinel AI Expert") },
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
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            when (val download = state.downloadState) {
                is DownloadState.Idle -> {
                    DownloadPrompt(
                        onDownload = { viewModel.startDownload() },
                        onImport = { importLauncher.launch("*/*") }
                    )
                }
                is DownloadState.Downloading -> {
                    DownloadProgress(progress = download.progress)
                }
                is DownloadState.Error -> {
                    Text("Error: ${download.message}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.startDownload() }, modifier = Modifier.padding(top = 8.dp)) { Text("Retry") }
                }
                is DownloadState.Completed -> {
                    if (state.selectedReportSsid != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = "Analyzing Audit: ${state.selectedReportSsid}",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showReportPicker = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Load from History")
                    }

                    AnalysisInterface(
                        inputData = state.inputData,
                        onInputChanged = { viewModel.onInputChanged(it) },
                        onAnalyze = { viewModel.analyzeNetworkLog() },
                        result = state.analysisResult,
                        isAnalyzing = state.isAnalyzing
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadPrompt(onDownload: () -> Unit, onImport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("AI Unit Required", style = MaterialTheme.typography.titleLarge)
            Text(
                "To enable on-device analysis, the Gemma 2 2B intelligence unit must be indexed (~1.6 GB).",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                Text("Download AI Sync")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Text("Import Local .bin File")
            }
        }
    }
}

@Composable
fun DownloadProgress(progress: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 32.dp)) {
        CircularProgressIndicator(progress = { progress / 100f }, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Synchronizing Logic Unit... $progress%", style = MaterialTheme.typography.titleMedium)
        Text("Stay connected to Wi-Fi", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun AnalysisInterface(
    inputData: String,
    onInputChanged: (String) -> Unit,
    onAnalyze: () -> Unit,
    result: String,
    isAnalyzing: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = inputData,
            onValueChange = onInputChanged,
            modifier = Modifier.fillMaxWidth().height(150.dp),
            label = { Text("Raw Scan Logs / Service Banners") },
            placeholder = { Text("Paste Nmap output or service banners here...") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAnalyzing && inputData.isNotBlank()
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze Vulnerabilities")
            }
        }
        
        if (result.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NetSentinel AI Report", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = result, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
