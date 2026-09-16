package com.example.networkscanner.ui.viewmodel

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkscanner.util.KalmanFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignalState(
    val ssid: String = "Scanning...",
    val bssid: String = "N/A",
    val rssi: Int = -100,
    val smoothedRssi: Double = -100.0,
    val signalStrengthPercentage: Int = 0,
    val history: List<Double> = emptyList()
)

@HiltViewModel
class SignalTrackerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val kalmanFilter = KalmanFilter()
    
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val _uiState = MutableStateFlow(SignalState())
    val uiState: StateFlow<SignalState> = _uiState.asStateFlow()

    private var trackingJob: Job? = null

    /**
     * Starts active tracking and haptic feedback.
     * Should only be called when the Signal Tracker screen is visible to the user.
     */
    fun startTracking() {
        if (trackingJob?.isActive == true) return
        android.util.Log.d("SignalTracker", "Tracking started")

        trackingJob = viewModelScope.launch(Dispatchers.IO) {
            var lastSignificantRssi = -100.0
            while (isActive) {
                @Suppress("DEPRECATION")
                val info = wifiManager.connectionInfo
                if (info != null && info.bssid != null) {
                    val rssi = info.rssi
                    val smoothed = kalmanFilter.update(rssi.toDouble())
                    
                    // Ultra-sensitive haptic pulses for near-field tracking
                    if (smoothed > lastSignificantRssi + 1.5) {
                        triggerHapticPulse()
                        lastSignificantRssi = smoothed
                    } else if (smoothed < lastSignificantRssi - 4.0) {
                        lastSignificantRssi = smoothed
                    }

                    val percentage = calculatePercentage(smoothed.toInt())
                    
                    val currentHistory = _uiState.value.history.toMutableList()
                    currentHistory.add(smoothed)
                    if (currentHistory.size > 50) currentHistory.removeAt(0)

                    _uiState.value = _uiState.value.copy(
                        ssid = info.ssid.removeSurrounding("\""),
                        bssid = info.bssid,
                        rssi = rssi,
                        smoothedRssi = smoothed,
                        signalStrengthPercentage = percentage,
                        history = currentHistory
                    )
                } else {
                    kalmanFilter.reset(-100.0)
                    _uiState.value = _uiState.value.copy(
                        ssid = "Not Connected",
                        bssid = "N/A",
                        rssi = -100,
                        smoothedRssi = -100.0,
                        signalStrengthPercentage = 0,
                        history = emptyList()
                    )
                }
                delay(100)
            }
        }
    }

    /**
     * Stops the background polling and haptic feedback.
     */
    fun stopTracking() {
        android.util.Log.d("SignalTracker", "Tracking stopped")
        trackingJob?.cancel()
        trackingJob = null
    }

    private fun triggerHapticPulse() {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun calculatePercentage(rssi: Int): Int {
        return ((rssi + 100).coerceIn(0, 70) * 100 / 70)
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
