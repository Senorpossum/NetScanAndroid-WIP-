package com.example.networkscanner.domain.service

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PortScannerTest {

    private val portScannerService = PortScannerService()

    @Test
    fun `test socket timeout failure isolation does not crash pool`() = runTest {
        // Scan a blackhole IP that will definitely timeout, ensuring it doesn't crash the coroutine
        val results = portScannerService.scanHost("192.0.2.1", timeoutMs = 50)
        
        // Ensure all ports were scanned and returned FILTERED_OR_CLOSED without throwing exceptions
        assertEquals(11, results.size)
        assert(results.all { it.state == PortState.FILTERED_OR_CLOSED })
    }
}
