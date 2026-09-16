package com.example.networkscanner.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

enum class RiskSeverity { LOW, MED, HIGH, UNKNOWN }
enum class PortState { OPEN, FILTERED_OR_CLOSED }

data class PortResult(
    val port: Int,
    val serviceName: String,
    val state: PortState,
    val riskSeverity: RiskSeverity
)

class PortScannerService {

    private val criticalPorts = mapOf(
        21 to Pair("FTP", RiskSeverity.HIGH),
        22 to Pair("SSH", RiskSeverity.HIGH),
        23 to Pair("Telnet", RiskSeverity.HIGH),
        53 to Pair("DNS", RiskSeverity.LOW),
        80 to Pair("HTTP", RiskSeverity.LOW),
        443 to Pair("HTTPS", RiskSeverity.LOW),
        445 to Pair("SMB", RiskSeverity.HIGH),
        554 to Pair("RTSP", RiskSeverity.MED),
        3389 to Pair("RDP", RiskSeverity.HIGH),
        5555 to Pair("ADB", RiskSeverity.HIGH),
        8080 to Pair("Web", RiskSeverity.MED)
    )

    // Performance: Allocate dedicated fast-pool for IO sockets
    private val scanDispatcher = Dispatchers.IO.limitedParallelism(128)

    suspend fun scanHost(ipAddress: String, timeoutMs: Int = 300): List<PortResult> = withContext(scanDispatcher) {
        coroutineScope {
            criticalPorts.map { (port, info) ->
                async {
                    ensureActive()
                    checkPort(ipAddress, port, info.first, info.second, timeoutMs)
                }
            }.awaitAll()
        }
    }

    private fun checkPort(
        ipAddress: String,
        port: Int,
        serviceName: String,
        severity: RiskSeverity,
        timeoutMs: Int
    ): PortResult {
        var channel: SocketChannel? = null
        return try {
            channel = SocketChannel.open()
            channel.configureBlocking(true)
            // Timeout handling and socket failure isolation handled by try-catch mapping to FILTERED_OR_CLOSED state
            channel.socket().connect(InetSocketAddress(ipAddress, port), timeoutMs)
            
            PortResult(port, serviceName, PortState.OPEN, severity)
        } catch (e: Exception) {
            PortResult(port, serviceName, PortState.FILTERED_OR_CLOSED, severity)
        } finally {
            try {
                channel?.close()
            } catch (e: Exception) {
                // Ignore close exceptions
            }
        }
    }
}
