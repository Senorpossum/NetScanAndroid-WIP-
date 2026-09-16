package com.example.networkscanner.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore

data class HostResult(
    val ipAddress: String,
    val macAddress: String?,
    val hostname: String?,
    val vendor: String? = null
)

class LanScannerRepository(private val context: Context) {

    // Performance: Balanced throughput with 32-thread limit to avoid system-wide lag
    private val scanDispatcher = Dispatchers.IO.limitedParallelism(32)

    suspend fun scanLocalNetwork(): List<HostResult> = withContext(scanDispatcher) {
        val (deviceIp, subnetPrefixLength) = getLocalNetworkInfo() ?: throw IllegalStateException("Not connected to a valid Wi-Fi/Ethernet network.")
        val allIpsToScan = generateIpsInSubnet(deviceIp, subnetPrefixLength)

        coroutineScope {
            allIpsToScan.map { ip ->
                async {
                    ensureActive()
                    val result = scanIp(ip)
                    // Pacing: Add microscopic jitter to spread socket load
                    delay(1)
                    result
                }
            }.awaitAll().filterNotNull()
        }
    }

    private fun scanIp(ipAddress: String): HostResult? {
        return try {
            val address = InetAddress.getByName(ipAddress)
            // Reachability test (Ping)
            if (address.isReachable(500)) {
                val macAddress = resolveMacAddress(ipAddress)
                HostResult(ipAddress, macAddress, address.canonicalHostName)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getLocalNetworkInfo(): Pair<String, Int>? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return null
        val capabilities = cm.getNetworkCapabilities(network) ?: return null
        
        // Edge Case Handling: Prevent scanning on VPN or Cellular when Wi-Fi is disconnected
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return null
        }
        
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return null

        val linkProperties = cm.getLinkProperties(network) ?: return null
        val linkAddress = linkProperties.linkAddresses.firstOrNull { it.address is Inet4Address } ?: return null
        
        return Pair(linkAddress.address.hostAddress ?: "", linkAddress.prefixLength)
    }

    // Visible for testing - CIDR block parsing support
    internal fun generateIpsInSubnet(ipAddress: String, prefixLength: Int): List<String> {
        if (prefixLength < 8 || prefixLength > 30) return emptyList()
        try {
            val address = InetAddress.getByName(ipAddress)
            val ipValue = ByteBuffer.wrap(address.address).int.toLong() and 0xFFFFFFFF
            val mask = (-1 shl (32 - prefixLength)).toLong() and 0xFFFFFFFF
            
            val network = ipValue and mask
            val broadcast = network or mask.inv() and 0xFFFFFFFF
            
            val ips = mutableListOf<String>()
            for (i in (network + 1) until broadcast) {
                val bytes = ByteBuffer.allocate(4).putInt(i.toInt()).array()
                ips.add(InetAddress.getByAddress(bytes).hostAddress ?: continue)
            }
            return ips
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun resolveMacAddress(ipAddress: String): String? {
        try {
            val arpTable = File("/proc/net/arp").readLines()
            for (line in arpTable) {
                if (line.contains(ipAddress)) {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        val mac = parts[3]
                        if (mac != "00:00:00:00:00:00") {
                            return mac
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Permission denied or file not found
        }
        return null
    }
}
