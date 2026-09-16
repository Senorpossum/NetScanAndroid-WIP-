package com.example.networkscanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkscanner.data.repository.WifiAuditRepository
import com.example.networkscanner.data.repository.WifiScanAudit
import com.example.networkscanner.domain.service.ScanningCoordinator
import com.example.networkscanner.ui.state.ScanUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WifiAuditViewModel @Inject constructor(
    private val scanningCoordinator: ScanningCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState<List<WifiScanAudit>>>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState<List<WifiScanAudit>>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scanningCoordinator.wifiAudits.collectLatest { audits ->
                if (audits.isNotEmpty()) {
                    _uiState.value = ScanUiState.Success(audits)
                }
            }
        }
    }

    fun startWifiAudit() {
        scanningCoordinator.startContinuousScanning()
    }
}
