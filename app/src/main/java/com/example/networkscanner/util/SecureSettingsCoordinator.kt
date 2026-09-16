package com.example.networkscanner.util

import android.view.Window
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SecureSettingsCoordinator {

    // Ephemeral Mode: When true, all scans are executed strictly in memory.
    // The Room Database insertion logic is completely bypassed.
    private val _isEphemeralModeEnabled = MutableStateFlow(false)
    val isEphemeralModeEnabled: StateFlow<Boolean> = _isEphemeralModeEnabled.asStateFlow()

    fun toggleEphemeralMode(enabled: Boolean) {
        _isEphemeralModeEnabled.value = enabled
    }

    /**
     * Toggles FLAG_SECURE on the current window.
     * When enabled, this completely blocks users, background apps, and the Android OS 
     * from taking screenshots, recording the screen, or saving a snapshot to the App Switcher.
     */
    fun setAntiSnapshotProtection(window: Window, enabled: Boolean) {
        if (enabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
