package com.example.networkscanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkscanner.data.local.DiscoveredHostEntity
import com.example.networkscanner.data.local.NetworkScannerDao
import com.example.networkscanner.data.local.NetworkSnapshotEntity
import com.example.networkscanner.data.repository.HostResult
import com.example.networkscanner.data.repository.LanScannerRepository
import com.example.networkscanner.data.repository.NetworkInfoRepository
import com.example.networkscanner.domain.model.DiscoveredHost
import com.example.networkscanner.domain.service.OuiResolver
import com.example.networkscanner.domain.service.PortResult
import com.example.networkscanner.domain.service.PortScannerService
import com.example.networkscanner.domain.service.ScanningCoordinator
import com.example.networkscanner.domain.service.VulnerabilityEngine
import com.example.networkscanner.ui.state.ScanUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.Immutable
import javax.inject.Inject

@Immutable
data class HostWithPorts(
    val host: HostResult,
    val openPorts: List<PortResult> = emptyList(),
    val securityAdvice: List<String> = emptyList()
)

@HiltViewModel
class LanScannerViewModel @Inject constructor(
    private val portScannerService: PortScannerService,
    private val networkInfoRepository: NetworkInfoRepository,
    private val scanningCoordinator: ScanningCoordinator,
    private val vulnerabilityEngine: VulnerabilityEngine,
    private val dao: NetworkScannerDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState<List<HostWithPorts>>>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState<List<HostWithPorts>>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scanningCoordinator.lanHosts.collectLatest { hosts ->
                if (hosts.isNotEmpty()) {
                    val currentSuccessData = (_uiState.value as? ScanUiState.Success)?.data ?: emptyList()
                    
                    // Merge new scan results while preserving port scan info for known IPs
                    val merged = hosts.map { host ->
                        val existing = currentSuccessData.find { it.host.ipAddress == host.ipAddress }
                        HostWithPorts(
                            host = host,
                            openPorts = existing?.openPorts ?: emptyList(),
                            securityAdvice = existing?.securityAdvice ?: emptyList()
                        )
                    }
                    _uiState.value = ScanUiState.Success(merged)
                }
            }
        }
    }

    fun scanNetwork() {
        // Manual trigger still works, but Coordinator is already doing it
        scanningCoordinator.startContinuousScanning()
    }

    fun saveCurrentSnapshot() {
        val currentHosts = (_uiState.value as? ScanUiState.Success)?.data?.map { it.host } ?: return
        viewModelScope.launch {
            saveSnapshot(currentHosts)
        }
    }

    fun scanPortsForHost(hostWithPorts: HostWithPorts) {
        val currentList = (_uiState.value as? ScanUiState.Success)?.data ?: return
        
        viewModelScope.launch {
            val results = portScannerService.scanHost(hostWithPorts.host.ipAddress)
            
            // Evaluate vulnerabilities
            val domainHost = DiscoveredHost(
                ipAddress = hostWithPorts.host.ipAddress,
                macAddress = hostWithPorts.host.macAddress,
                vendorOui = hostWithPorts.host.vendor,
                hostname = hostWithPorts.host.hostname,
                openPorts = results.map { it.port },
                isNewDevice = false
            )
            val report = vulnerabilityEngine.evaluateHost(domainHost)

            val updatedList = currentList.map { 
                if (it.host.ipAddress == hostWithPorts.host.ipAddress) {
                    it.copy(openPorts = results, securityAdvice = report.remediationAdvice)
                } else it
            }
            _uiState.value = ScanUiState.Success(updatedList)
        }
    }

    private suspend fun saveSnapshot(hosts: List<HostResult>) {
        val info = networkInfoRepository.observeNetworkInfo().first()
        val snapshotId = dao.insertSnapshot(
            NetworkSnapshotEntity(
                timestamp = System.currentTimeMillis(),
                ssid = info.ssid,
                bssid = "N/A",
                gatewayIp = info.gateway,
                subnetMask = "255.255.255.0",
                deviceCount = hosts.size
            )
        )
        
        val entities = hosts.map { host ->
            DiscoveredHostEntity(
                snapshotId = snapshotId,
                ipAddress = host.ipAddress,
                macAddress = host.macAddress,
                vendorOui = host.vendor,
                hostname = host.hostname,
                openPorts = emptyList(),
                isNewDevice = true
            )
        }
        dao.insertHosts(entities)
    }
}
