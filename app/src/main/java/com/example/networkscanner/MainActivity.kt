package com.example.networkscanner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.networkscanner.ui.navigation.RootScaffold
import com.example.networkscanner.ui.theme.NetworkScannerTheme
import com.example.networkscanner.util.SecureSettingsCoordinator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Extended from FragmentActivity to allow androidx.biometric.BiometricPrompt binding
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var scanningCoordinator: com.example.networkscanner.domain.service.ScanningCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start background auto-scanning as requested
        scanningCoordinator.startContinuousScanning()
        
        // Draw the UI edge-to-edge behind the system navigation and status bars
        enableEdgeToEdge()
        
        // Apply FLAG_SECURE immediately to prevent the OS from snapshotting the loading screen
        SecureSettingsCoordinator.setAntiSnapshotProtection(window, true)
        
        setContent {
            NetworkScannerTheme {
                RootScaffold()
            }
        }
    }
}
