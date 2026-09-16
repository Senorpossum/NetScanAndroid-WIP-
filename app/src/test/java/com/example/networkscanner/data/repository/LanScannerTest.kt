package com.example.networkscanner.data.repository

import android.content.Context
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LanScannerTest {

    private val context = mockk<Context>(relaxed = true)
    private val repository = LanScannerRepository(context)

    @Test
    fun `test CIDR calculation for 24 mask`() {
        val ips = repository.generateIpsInSubnet("192.168.1.100", 24)
        assertEquals(254, ips.size)
        assertEquals("192.168.1.1", ips.first())
        assertEquals("192.168.1.254", ips.last())
    }

    @Test
    fun `test CIDR calculation for 16 mask`() {
        val ips = repository.generateIpsInSubnet("10.0.0.50", 16)
        assertEquals(65534, ips.size)
        assertEquals("10.0.0.1", ips.first())
        assertEquals("10.0.255.254", ips.last())
    }

    @Test
    fun `test CIDR calculation for 28 mask`() {
        val ips = repository.generateIpsInSubnet("192.168.1.100", 28)
        assertEquals(14, ips.size)
        // 192.168.1.100 /28 -> Network: 192.168.1.96, Broadcast: 192.168.1.111
        // Range: .97 to .110
        assertEquals("192.168.1.97", ips.first())
        assertEquals("192.168.1.110", ips.last())
    }
}
