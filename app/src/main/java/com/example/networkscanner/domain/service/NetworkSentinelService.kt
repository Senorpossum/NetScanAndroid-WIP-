package com.example.networkscanner.domain.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import android.os.PowerManager
import com.example.networkscanner.data.repository.NetworkInfoRepository
import com.example.networkscanner.util.SentinelNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class NetworkSentinelService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sweepJob: Job? = null
    
    @Inject lateinit var notificationManager: SentinelNotificationManager
    @Inject lateinit var arpDetector: ArpSpoofDetector
    @Inject lateinit var networkInfoRepository: NetworkInfoRepository
    
    private lateinit var powerManager: PowerManager
    private lateinit var connectivityManager: ConnectivityManager

    // Configuration
    private val SWEEP_INTERVAL_MS = 15 * 60 * 1000L // 15 Minutes

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> evaluateSweepState()
                ConnectivityManager.CONNECTIVITY_ACTION -> evaluateSweepState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Register strict battery and network state listeners to prevent drain
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        registerReceiver(stateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Elevate immediately to Foreground Service to prevent OS killing the process
        startForeground(
            SentinelNotificationManager.NOTIFICATION_ID_FOREGROUND,
            notificationManager.getForegroundNotification("Sentinel initialized.")
        )
        
        evaluateSweepState()

        return START_STICKY
    }

    private fun evaluateSweepState() {
        val isPowerSaveMode = powerManager.isPowerSaveMode
        val isWifiConnected = isConnectedToWifi()

        if (isPowerSaveMode || !isWifiConnected) {
            pauseSweeps()
            val reason = if (isPowerSaveMode) "Battery Saver" else "Not on Wi-Fi"
            notificationManager.updateForegroundNotification("Sweeps paused ($reason)")
        } else {
            startSweeps()
            notificationManager.updateForegroundNotification("Actively monitoring subnet...")
        }
    }

    private fun startSweeps() {
        if (sweepJob?.isActive == true) return

        sweepJob = serviceScope.launch {
            while (isActive) {
                performSentinelSweep()
                delay(SWEEP_INTERVAL_MS)
            }
        }
    }

    private fun pauseSweeps() {
        sweepJob?.cancel()
        sweepJob = null
    }

    private suspend fun performSentinelSweep() {
        // 1. Execute ARP Table Dump
        val arpTable = arpDetector.readArpTable()
        
        // 2. Fetch Gateway IP from WifiManager (Mocked for scaffold)
        val gatewayIp = "192.168.1.1" 

        // 3. Detect Man-in-the-Middle (MitM) Attacks
        val mitmAlert = arpDetector.detectMitM(gatewayIp, arpTable)
        if (mitmAlert != null) {
            notificationManager.dispatchArpSpoofAlert(mitmAlert)
        }

        // 4. Rogue Device Detection (Scaffold)
        // Here you would cross-reference `arpTable` MACs against the Room Database.
        // If a MAC is not historically recognized: 
        // notificationManager.dispatchRogueDeviceAlert(mac, vendor)
    }

    private fun isConnectedToWifi(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stateReceiver)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
