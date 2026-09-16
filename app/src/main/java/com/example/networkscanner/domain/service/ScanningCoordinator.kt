package com.example.networkscanner.domain.service

import com.example.networkscanner.data.repository.HostResult
import com.example.networkscanner.data.repository.LanScannerRepository
import com.example.networkscanner.data.repository.WifiAuditRepository
import com.example.networkscanner.data.repository.WifiScanAudit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanningCoordinator @Inject constructor(
    private val lanScannerRepository: LanScannerRepository,
    private val wifiAuditRepository: WifiAuditRepository,
    private val ouiResolver: OuiResolver
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val resourceMutex = Mutex()
    
    private val _lanHosts = MutableStateFlow<List<HostResult>>(emptyList())
    val lanHosts: StateFlow<List<HostResult>> = _lanHosts.asStateFlow()

    private val _wifiAudits = MutableStateFlow<List<WifiScanAudit>>(emptyList())
    val wifiAudits: StateFlow<List<WifiScanAudit>> = _wifiAudits.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanJob: Job? = null

    /**
     * Starts continuous LAN and Wi-Fi scanning.
     * Uses a Mutex to ensure it doesn't run during heavy network tests.
     */
    fun startContinuousScanning() {
        if (scanJob?.isActive == true) return
        
        scanJob = scope.launch {
            _isScanning.value = true
            while (isActive) {
                // Wait for any exclusive resource locks (like Bufferbloat test) to clear
                resourceMutex.withLock {
                    coroutineScope {
                        launch {
                            try {
                                val hosts = lanScannerRepository.scanLocalNetwork()
                                val resolved = hosts.map { it.copy(vendor = ouiResolver.resolveVendor(it.macAddress)) }
                                _lanHosts.value = resolved
                            } catch (e: Exception) {}
                        }
                        
                        launch {
                            try {
                                wifiAuditRepository.scanWifiNetworks().first().let {
                                    _wifiAudits.value = it
                                }
                            } catch (e: Exception) {}
                        }
                    }
                }
                delay(5000)
            }
        }
    }

    /**
     * Provides exclusive access to network resources. 
     * Use this to run heavy tests (Bufferbloat) while pausing background scans.
     */
    suspend fun <T> withExclusiveNetworkAccess(block: suspend () -> T): T {
        return resourceMutex.withLock {
            block()
        }
    }

    fun stopContinuousScanning() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
    }
}
