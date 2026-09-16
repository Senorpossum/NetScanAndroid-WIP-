package com.example.networkscanner.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import androidx.compose.runtime.Immutable
import java.lang.Exception

@Immutable
data class WifiScanAudit(
    val ssid: String,
    val bssid: String,
    val capabilities: String,
    val frequency: Int,
    val rssi: Int,
    val isPotentialRogue: Boolean
)

class WifiAuditRepository(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun scanWifiNetworks(): Flow<List<WifiScanAudit>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    try {
                        // Location permissions are required to access scanResults
                        val results = wifiManager.scanResults
                        if (results.isNotEmpty()) {
                            trySend(analyzeScanResults(results))
                        } else if (!success) {
                            close(Exception("Wi-Fi scan failed and cache is empty. You may be throttled by the OS."))
                        }
                    } catch (e: SecurityException) {
                        close(e)
                    }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        
        // Edge Case Handling: Android Wi-Fi scan throttling
        try {
            @Suppress("DEPRECATION")
            val scanStarted = wifiManager.startScan()
            if (!scanStarted) {
                // Throttled. Fallback to cached results.
                val cachedResults = wifiManager.scanResults
                if (cachedResults.isNotEmpty()) {
                    trySend(analyzeScanResults(cachedResults))
                } else {
                    close(Exception("Wi-Fi scan throttled by OS and no cached results available."))
                }
            }
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    // Visible for testing
    internal fun analyzeScanResults(results: List<ScanResult>): List<WifiScanAudit> {
        val audits = mutableListOf<WifiScanAudit>()
        val groupedBySsid = results.groupBy { it.SSID }

        for (result in results) {
            val ssid = result.SSID ?: "Hidden"
            if (ssid.isEmpty()) continue
            
            val peers = groupedBySsid[ssid] ?: emptyList()
            var isRogue = false
            
            if (peers.size > 1) {
                val baseCapability = peers.first().capabilities
                val hasMismatchedEncryption = peers.any { it.capabilities != baseCapability }
                
                if (hasMismatchedEncryption) {
                    isRogue = true
                }
            }

            audits.add(
                WifiScanAudit(
                    ssid = ssid,
                    bssid = result.BSSID ?: "Unknown",
                    capabilities = result.capabilities ?: "Unknown",
                    frequency = result.frequency,
                    rssi = result.level,
                    isPotentialRogue = isRogue
                )
            )
        }
        
        return audits.sortedByDescending { it.rssi }
    }
}
