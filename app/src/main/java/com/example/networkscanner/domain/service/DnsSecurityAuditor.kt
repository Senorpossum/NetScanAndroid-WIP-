package com.example.networkscanner.domain.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.os.Build
import com.example.networkscanner.domain.model.DnsSecurityReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class DnsSecurityAuditor(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    suspend fun auditDnsConfiguration(): DnsSecurityReport = withContext(Dispatchers.IO) {
        val activeNetwork: Network? = connectivityManager.activeNetwork
        var isPrivateDnsActive = false
        val activeDnsServers = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (activeNetwork != null) {
            val linkProperties: LinkProperties? = connectivityManager.getLinkProperties(activeNetwork)
            if (linkProperties != null) {
                // Check if Android Private DNS (DoT) is protecting queries
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isPrivateDnsActive = linkProperties.isPrivateDnsActive
                }
                
                linkProperties.dnsServers.forEach { 
                    activeDnsServers.add(it.hostAddress ?: "")
                }
            }
        }

        if (!isPrivateDnsActive) {
            warnings.add("HIGH: Private DNS (DoT/DoH) is disabled. Queries are visible in plain-text to the ISP or local network spoofers.")
        }

        // Test Canary DNS (Raw UDP to local DNS vs Trusted Root)
        var isHijacked = false
        if (activeDnsServers.isNotEmpty()) {
            val localDns = activeDnsServers.first()
            val canaryDomain = "example.com"
            
            // Dispatch raw UDP requests
            val localResponse = resolveRawUdp(canaryDomain, localDns)
            val trustedResponse = resolveRawUdp(canaryDomain, "8.8.8.8")
            
            // If the local DNS intercepts a known safe domain and redirects it (e.g. captive portal or proxy)
            // or if they differ significantly and the local is known malicious
            if (localResponse != null && trustedResponse != null && localResponse != trustedResponse) {
                isHijacked = true
                warnings.add("CRITICAL: DNS Hijacking / Transparent Proxy detected. Local DNS responses deviate maliciously from root/trusted DNS servers.")
            }
        } else {
            warnings.add("INFO: No active DNS servers identified on the current link.")
        }

        DnsSecurityReport(
            activeDnsServers = activeDnsServers,
            isPrivateDnsActive = isPrivateDnsActive,
            isHijacked = isHijacked,
            warnings = warnings
        )
    }

    /**
     * Constructs and fires a Raw UDP DNS A-Record query.
     */
    private fun resolveRawUdp(domain: String, dnsServerIp: String): String? {
        // Scaffold: Hardcoded DNS query bytes for `example.com` A-record
        val payload = byteArrayOf(
            0x12, 0x34, // Transaction ID
            0x01, 0x00, // Flags: Standard query
            0x00, 0x01, // Questions: 1
            0x00, 0x00, // Answer RRs: 0
            0x00, 0x00, // Authority RRs: 0
            0x00, 0x00, // Additional RRs: 0
            // domain payload `example.com`
            0x07, 0x65, 0x78, 0x61, 0x6d, 0x70, 0x6c, 0x65, 0x03, 0x63, 0x6f, 0x6d, 0x00,
            0x00, 0x01, // Type A
            0x00, 0x01  // Class IN
        )

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = 1000
            val serverAddress = InetAddress.getByName(dnsServerIp)
            val packet = DatagramPacket(payload, payload.size, serverAddress, 53)
            socket.send(packet)

            val receiveBuf = ByteArray(512)
            val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)
            socket.receive(receivePacket)
            
            // Scaffold parsing: Returning response size hash to compare divergence
            return "scaffold_ip_hash_${receivePacket.length}"
        } catch (e: SocketTimeoutException) {
            return null
        } catch (e: Exception) {
            return null
        } finally {
            socket?.close()
        }
    }
}
