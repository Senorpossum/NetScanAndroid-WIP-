package com.example.networkscanner.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

data class SsdpDevice(
    val ipAddress: String,
    val locationUrl: String,
    val serverBanner: String
)

class SsdpDiscoveryService {

    suspend fun discoverDevices(timeoutMs: Int = 3000): List<SsdpDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<SsdpDevice>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = timeoutMs

            val mSearchPayload = """
                M-SEARCH * HTTP/1.1
                HOST: 239.255.255.250:1900
                MAN: "ssdp:discover"
                MX: 1
                ST: ssdp:all
                
            """.trimIndent().replace("\n", "\r\n") + "\r\n"

            val buffer = mSearchPayload.toByteArray()
            val multicastGroup = InetAddress.getByName("239.255.255.250")
            val packet = DatagramPacket(buffer, buffer.size, multicastGroup, 1900)

            // Emit M-SEARCH UDP multicast probe
            socket.send(packet)

            val receiveBuffer = ByteArray(2048)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    
                    val ip = receivePacket.address.hostAddress ?: continue
                    val location = extractHeader(response, "LOCATION") ?: ""
                    val server = extractHeader(response, "SERVER") ?: ""
                    
                    if (location.isNotEmpty() || server.isNotEmpty()) {
                        devices.add(SsdpDevice(ip, location, server))
                    }
                } catch (e: SocketTimeoutException) {
                    break // Timeout reached naturally
                }
            }
        } catch (e: Exception) {
            // Silent catch to prevent crashing concurrent scanning flows
        } finally {
            socket?.close()
        }
        
        devices.distinctBy { it.ipAddress }
    }

    private fun extractHeader(response: String, headerName: String): String? {
        val lines = response.split("\r\n", "\n")
        for (line in lines) {
            if (line.startsWith("$headerName:", ignoreCase = true)) {
                return line.substringAfter(":").trim()
            }
        }
        return null
    }
}
