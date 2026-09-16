package com.example.networkscanner.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WakeOnLanService {

    /**
     * Constructs and broadcasts a standard Wake-on-LAN Magic Packet.
     * 
     * @param targetMac The physical MAC address of the target NIC (e.g. "00:11:22:33:44:55")
     * @param broadcastIp The subnet broadcast address (usually "255.255.255.255")
     * @return Boolean representing if the packet was successfully dispatched
     */
    suspend fun sendMagicPacket(targetMac: String, broadcastIp: String = "255.255.255.255"): Boolean = withContext(Dispatchers.IO) {
        try {
            val macBytes = getMacBytes(targetMac)
            require(macBytes.size == 6) { "Invalid MAC address format" }

            // A Magic Packet is 6 bytes of 0xFF followed by the MAC address repeated 16 times.
            val payload = ByteArray(6 + 16 * macBytes.size)
            for (i in 0 until 6) {
                payload[i] = 0xff.toByte()
            }
            for (i in 6 until payload.size step macBytes.size) {
                System.arraycopy(macBytes, 0, payload, i, macBytes.size)
            }

            val address = InetAddress.getByName(broadcastIp)
            
            // Broadcast over standard WoL ports (7 & 9)
            DatagramSocket().use { socket ->
                socket.broadcast = true
                val packetPort7 = DatagramPacket(payload, payload.size, address, 7)
                val packetPort9 = DatagramPacket(payload, payload.size, address, 9)
                
                socket.send(packetPort7)
                socket.send(packetPort9)
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun getMacBytes(macStr: String): ByteArray {
        val hexStrings = macStr.split(":", "-")
        val bytes = ByteArray(hexStrings.size)
        for (i in hexStrings.indices) {
            bytes[i] = Integer.parseInt(hexStrings[i], 16).toByte()
        }
        return bytes
    }
}
