package com.example.networkscanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkscanner.data.repository.NetworkInfoRepository
import com.example.networkscanner.domain.service.BufferbloatAuditor
import com.example.networkscanner.domain.service.BufferbloatResult
import com.example.networkscanner.domain.service.LatencyMetrics
import com.example.networkscanner.domain.service.LatencyProfiler
import com.example.networkscanner.domain.service.ScanningCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkHealthViewModel @Inject constructor(
    private val latencyProfiler: LatencyProfiler,
    private val bufferbloatAuditor: BufferbloatAuditor,
    private val networkInfoRepository: NetworkInfoRepository,
    private val scanningCoordinator: ScanningCoordinator
) : ViewModel() {

    private val _idleMetrics = MutableStateFlow<LatencyMetrics?>(null)
    val idleMetrics: StateFlow<LatencyMetrics?> = _idleMetrics.asStateFlow()

    private val _bufferbloatResult = MutableStateFlow<BufferbloatResult?>(null)
    val bufferbloatResult: StateFlow<BufferbloatResult?> = _bufferbloatResult.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private var monitoringJob: Job? = null

    /**
     * Starts background latency monitoring for the live oscilloscope.
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (!_isTesting.value) {
                    val metrics = latencyProfiler.profileTarget("8.8.8.8", burstCount = 10)
                    _idleMetrics.value = metrics
                }
                delay(2000)
            }
        }
    }

    /**
     * Stops the background monitoring job.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    fun runLoadTest() {
        if (_isTesting.value) return
        
        viewModelScope.launch {
            _isTesting.value = true
            
            // Acquire exclusive access to network resources to prevent scanner interference
            scanningCoordinator.withExclusiveNetworkAccess {
                try {
                    val info = networkInfoRepository.observeNetworkInfo().first()
                    val gateway = if (info.gateway != "Unknown") info.gateway else "8.8.8.8"
                    val result = bufferbloatAuditor.runAudit(gateway)
                    _bufferbloatResult.value = result
                } catch (e: Exception) {
                    // Fail silently or log
                } finally {
                    _isTesting.value = false
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
