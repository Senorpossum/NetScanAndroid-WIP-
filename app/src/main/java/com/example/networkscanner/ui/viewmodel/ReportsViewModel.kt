package com.example.networkscanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkscanner.data.local.NetworkScannerDao
import com.example.networkscanner.data.local.SnapshotWithHosts
import com.example.networkscanner.domain.model.DiscoveredHost
import com.example.networkscanner.domain.model.NetworkSnapshot
import com.example.networkscanner.domain.service.AuditPdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val dao: NetworkScannerDao,
    private val pdfExporter: AuditPdfExporter
) : ViewModel() {

    val snapshots: StateFlow<List<SnapshotWithHosts>> = dao.getAllSnapshotsWithHostsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun exportReport(snapshotWithHosts: SnapshotWithHosts, onFinished: (File?) -> Unit) {
        viewModelScope.launch {
            val domainSnapshot = mapToDomain(snapshotWithHosts)
            val file = pdfExporter.generateReport(domainSnapshot)
            onFinished(file)
        }
    }

    private fun mapToDomain(snapshotWithHosts: SnapshotWithHosts): NetworkSnapshot {
        val s = snapshotWithHosts.snapshot
        return NetworkSnapshot(
            id = s.id,
            timestamp = s.timestamp,
            ssid = s.ssid,
            bssid = s.bssid,
            gatewayIp = s.gatewayIp,
            subnetMask = s.subnetMask,
            deviceCount = s.deviceCount,
            discoveredHosts = snapshotWithHosts.hosts.map { h ->
                DiscoveredHost(
                    id = h.id,
                    ipAddress = h.ipAddress,
                    macAddress = h.macAddress,
                    vendorOui = h.vendorOui,
                    hostname = h.hostname,
                    openPorts = h.openPorts,
                    isNewDevice = h.isNewDevice
                )
            }
        )
    }
}
