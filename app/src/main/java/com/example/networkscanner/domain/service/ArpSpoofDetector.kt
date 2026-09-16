package com.example.networkscanner.domain.service

import java.io.BufferedReader
import java.io.FileReader

data class ArpEntry(val ip: String, val mac: String)

class ArpSpoofDetector {

    // Cache to hold the known legitimate Gateway MAC address from the first baseline sweep
    private var legitimateGatewayMac: String? = null

    /**
     * Reads /proc/net/arp or executes an `ip neigh` equivalent to extract current mappings.
     * Note: In Android 10+, /proc/net/arp is heavily restricted for non-system apps. 
     * This scaffolding represents the logical heuristics engine.
     */
    fun readArpTable(): List<ArpEntry> {
        val entries = mutableListOf<ArpEntry>()
        try {
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                reader.readLine() // Skip header row
                var line = reader.readLine()
                while (line != null) {
                    val tokens = line.split("\\s+".toRegex())
                    if (tokens.size >= 4) {
                        val ip = tokens[0]
                        val mac = tokens[3].uppercase()
                        if (mac != "00:00:00:00:00:00") {
                            entries.add(ArpEntry(ip, mac))
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            // Fallback: In production, parsing `ip neigh` or UDP reachability responses
        }
        return entries
    }

    /**
     * Detects MitM attacks by inspecting the ARP table for gateway shifts or MAC collisions.
     * Returns an alert message if spoofing is detected, or null if secure.
     */
    fun detectMitM(gatewayIp: String, currentArpTable: List<ArpEntry>): String? {
        val gatewayEntry = currentArpTable.find { it.ip == gatewayIp }
        
        // 1. Initial baseline setup
        if (legitimateGatewayMac == null && gatewayEntry != null) {
            legitimateGatewayMac = gatewayEntry.mac
        }

        // 2. Gateway Shift Detection (Heuristic 1)
        if (gatewayEntry != null && legitimateGatewayMac != null) {
            if (gatewayEntry.mac != legitimateGatewayMac) {
                return "Gateway MAC address abruptly changed from $legitimateGatewayMac to ${gatewayEntry.mac}. " +
                       "This is a strong indicator of an active ARP Poisoning (MitM) attack."
            }
        }

        // 3. MAC Collision Detection (Heuristic 2)
        // If two different IPs claim the same MAC address, one is spoofing the other.
        // Specifically check if a local IP is claiming the Gateway's MAC.
        if (legitimateGatewayMac != null) {
            val devicesClaimingGatewayMac = currentArpTable.filter { it.mac == legitimateGatewayMac }
            if (devicesClaimingGatewayMac.size > 1) {
                val ips = devicesClaimingGatewayMac.joinToString { it.ip }
                return "MAC Collision: Multiple IP addresses ($ips) are sharing the Gateway's MAC address ($legitimateGatewayMac). " +
                       "A rogue device is intercepting subnet traffic."
            }
        }

        return null
    }

    fun resetBaseline() {
        legitimateGatewayMac = null
    }
}
