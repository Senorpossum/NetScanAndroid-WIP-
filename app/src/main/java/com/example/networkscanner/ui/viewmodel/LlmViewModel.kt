package com.example.networkscanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkscanner.data.local.NetworkScannerDao
import com.example.networkscanner.data.local.SnapshotWithHosts
import com.example.networkscanner.domain.service.DownloadState
import com.example.networkscanner.domain.service.LlmInferenceEngine
import com.example.networkscanner.domain.service.ModelDownloadManager
import com.example.networkscanner.domain.service.NetworkLogAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LlmUiState(
    val downloadState: DownloadState = DownloadState.Idle,
    val analysisResult: String = "",
    val isAnalyzing: Boolean = false,
    val inputData: String = "",
    val selectedReportSsid: String? = null,
    val error: String? = null,
    val savedSnapshots: List<SnapshotWithHosts> = emptyList()
)

@HiltViewModel
class LlmViewModel @Inject constructor(
    private val downloadManager: ModelDownloadManager,
    private val llmEngine: LlmInferenceEngine,
    private val networkAnalyzer: NetworkLogAnalyzer,
    private val dao: NetworkScannerDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LlmUiState())
    val uiState: StateFlow<LlmUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadManager.downloadState.collectLatest { state ->
                _uiState.value = _uiState.value.copy(downloadState = state)
                if (state is DownloadState.Completed) {
                    val success = llmEngine.initialize(downloadManager.getModelPath())
                    if (!success) {
                        _uiState.value = _uiState.value.copy(error = "Protocol Error: LLM Initialization Refused.")
                    }
                }
            }
        }
        
        viewModelScope.launch {
            dao.getAllSnapshotsWithHostsFlow().collectLatest { snapshots ->
                _uiState.value = _uiState.value.copy(savedSnapshots = snapshots)
            }
        }
        
        // Initial check for existing model
        viewModelScope.launch {
            if (downloadManager.isModelDownloaded() && !llmEngine.isInitialized()) {
                val success = llmEngine.initialize(downloadManager.getModelPath())
                if (success) {
                    _uiState.value = _uiState.value.copy(downloadState = DownloadState.Completed(android.net.Uri.EMPTY))
                } else {
                    _uiState.value = _uiState.value.copy(error = "Sync required: Model corrupted or incompatible.")
                }
            } else if (llmEngine.isInitialized()) {
                _uiState.value = _uiState.value.copy(downloadState = DownloadState.Completed(android.net.Uri.EMPTY))
            }
        }
    }

    fun startDownload() {
        _uiState.value = _uiState.value.copy(error = null)
        viewModelScope.launch {
            downloadManager.downloadModel()
        }
    }

    fun importModel(uri: android.net.Uri) {
        _uiState.value = _uiState.value.copy(error = null)
        viewModelScope.launch {
            downloadManager.importModel(uri)
        }
    }

    fun onInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(inputData = input)
    }

    fun analyzeNetworkLog() {
        val input = _uiState.value.inputData
        if (input.isBlank() || _uiState.value.isAnalyzing) return

        _uiState.value = _uiState.value.copy(isAnalyzing = true, analysisResult = "")
        viewModelScope.launch {
            try {
                networkAnalyzer.analyzeScanOutput(input).collect { chunk ->
                    _uiState.value = _uiState.value.copy(
                        analysisResult = _uiState.value.analysisResult + chunk
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(analysisResult = "Analysis Interrupted: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isAnalyzing = false)
            }
        }
    }

    fun loadAndAnalyzeReport(snapshotId: Long) {
        viewModelScope.launch {
            val snapshotWithHosts = dao.getSnapshotById(snapshotId) ?: return@launch
            val formattedData = formatReportForAi(snapshotWithHosts)
            _uiState.value = _uiState.value.copy(
                inputData = formattedData,
                selectedReportSsid = snapshotWithHosts.snapshot.ssid
            )
            analyzeNetworkLog()
        }
    }

    private fun formatReportForAi(report: SnapshotWithHosts): String {
        val builder = StringBuilder()
        builder.append("Network Scan Report for SSID: ${report.snapshot.ssid}\n")
        builder.append("Gateway: ${report.snapshot.gatewayIp}\n")
        builder.append("Devices Found: ${report.hosts.size}\n\n")
        
        report.hosts.forEach { host ->
            builder.append("IP: ${host.ipAddress}\n")
            builder.append("Hostname: ${host.hostname ?: "Unknown"}\n")
            builder.append("Vendor: ${host.vendorOui ?: "Unknown"}\n")
            if (host.openPorts.isNotEmpty()) {
                builder.append("Open Ports: ${host.openPorts.joinToString(", ")}\n")
            }
            host.bannerString?.let { builder.append("Banner: $it\n") }
            builder.append("---\n")
        }
        return builder.toString()
    }
}
