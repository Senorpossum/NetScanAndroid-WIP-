package com.example.networkscanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkscanner.data.repository.NetworkInfoRepository
import com.example.networkscanner.domain.service.ScanningCoordinator
import com.example.networkscanner.domain.service.SentinelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val currentSsid: String = "Unknown",
    val gateway: String = "Unknown",
    val localIp: String = "Unknown",
    val externalIp: String = "Fetching...",
    val subnetRange: String = "Unknown",
    val totalHostsFound: Int = 0,
    val vulnerablePortsExposed: Int = 0,
    val rogueApWarnings: Int = 0,
    val healthScore: Int = 100
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val networkInfoRepository: NetworkInfoRepository,
    private val scanningCoordinator: ScanningCoordinator,
    private val sentinelManager: SentinelManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    private val _isSentinelActive = MutableStateFlow(false)
    val isSentinelActive: StateFlow<Boolean> = _isSentinelActive.asStateFlow()
    
    init {
        viewModelScope.launch {
            networkInfoRepository.observeNetworkInfo().collectLatest { info ->
                _uiState.value = _uiState.value.copy(
                    currentSsid = info.ssid,
                    gateway = info.gateway,
                    localIp = info.localIp,
                    subnetRange = info.subnetRange
                )
            }
        }

        viewModelScope.launch {
            combine(
                scanningCoordinator.lanHosts,
                scanningCoordinator.wifiAudits
            ) { hosts, audits ->
                val rogueCount = audits.count { it.isPotentialRogue }
                val score = calculateHealthScore(hosts.size, rogueCount)
                
                _uiState.value.copy(
                    totalHostsFound = hosts.size,
                    rogueApWarnings = rogueCount,
                    healthScore = score
                )
            }.collectLatest { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun calculateHealthScore(hostCount: Int, rogueCount: Int): Int {
        var score = 100
        score -= (rogueCount * 30)
        if (hostCount > 25) score -= 15
        return score.coerceIn(0, 100)
    }

    fun toggleSentinel(active: Boolean) {
        if (active) {
            sentinelManager.startSentinel()
        } else {
            sentinelManager.stopSentinel()
        }
        _isSentinelActive.value = active
    }
}
