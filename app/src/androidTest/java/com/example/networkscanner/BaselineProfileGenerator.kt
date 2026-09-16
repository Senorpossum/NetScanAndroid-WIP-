package com.example.networkscanner

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the baseline-prof.txt file.
 * Android ART uses this file during app installation to Ahead-Of-Time (AOT) compile 
 * these critical paths, resulting in significantly faster cold starts and smoother 
 * Jetpack Compose Canvas rendering.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.example.networkscanner",
        profileBlock = {
            // 1. Cold Startup (CRITICAL USER JOURNEY)
            // Launches the MainActivity and waits for the initial frame to render
            pressHome()
            startActivityAndWait()

            // Wait for the UI Root Scaffold to become visible
            device.waitForIdle()
            
            // 2. Navigate to LAN Scanner (AOT Compile the LazyColumn)
            val lanScannerTab = device.findObject(By.text("LAN Scanner"))
            lanScannerTab?.click()
            device.waitForIdle()

            // 3. Navigate to Diagnostics (AOT Compile the Canvas Oscilloscope)
            val diagnosticsTab = device.findObject(By.text("QoS & Health"))
            diagnosticsTab?.click()
            device.waitForIdle()
        }
    )
}
