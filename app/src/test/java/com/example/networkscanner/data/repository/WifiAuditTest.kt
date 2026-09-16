package com.example.networkscanner.data.repository

import android.content.Context
import android.net.wifi.ScanResult
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class WifiAuditTest {

    private val context = mockk<Context>(relaxed = true)
    private val repository = WifiAuditRepository(context)

    @Test
    fun `test Evil Twin detection heuristic flags mismatched encryption`() {
        // Create mock scan results sharing the same SSID
        val result1 = mockk<ScanResult>(relaxed = true).apply {
            SSID = "CorpGuest"
            BSSID = "00:11:22:33:44:55"
            capabilities = "[WPA3-SAE-CCMP]"
            level = -50
        }
        val result2 = mockk<ScanResult>(relaxed = true).apply {
            SSID = "CorpGuest"
            BSSID = "66:77:88:99:AA:BB"
            capabilities = "[ESS]" // Open network
            level = -45
        }

        val audits = repository.analyzeScanResults(listOf(result1, result2))
        
        assertEquals(2, audits.size)
        assertTrue(audits.all { it.isPotentialRogue })
    }

    @Test
    fun `test valid networks are not flagged`() {
        val result1 = mockk<ScanResult>(relaxed = true).apply {
            SSID = "HomeNet"
            BSSID = "11:22:33:44:55:66"
            capabilities = "[WPA2-PSK-CCMP]"
            level = -60
        }
        val result2 = mockk<ScanResult>(relaxed = true).apply {
            SSID = "HomeNet"
            BSSID = "11:22:33:44:55:77" // Different AP, same mesh
            capabilities = "[WPA2-PSK-CCMP]"
            level = -65
        }

        val audits = repository.analyzeScanResults(listOf(result1, result2))
        
        assertEquals(2, audits.size)
        assertFalse(audits.any { it.isPotentialRogue })
    }
}
