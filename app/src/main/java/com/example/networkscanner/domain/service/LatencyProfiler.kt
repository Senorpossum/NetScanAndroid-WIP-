package com.example.networkscanner.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

data class LatencyMetrics(
    val meanRtt: Float,
    val medianRtt: Float,
    val minRtt: Long,
    val maxRtt: Long,
    val jitter: Float,
    val packetLossPercent: Float,
    val rawSamples: List<Long>
)

class LatencyProfiler {

    suspend fun profileTarget(
        ipAddress: String, 
        port: Int = 53, // Target DNS/Gateway port
        burstCount: Int = 50,
        timeoutMs: Int = 150
    ): LatencyMetrics = withContext(Dispatchers.IO) {
        val samples = mutableListOf<Long>()
        var lostPackets = 0

        for (i in 0 until burstCount) {
            val rtt = measureTcpPing(ipAddress, port, timeoutMs)
            if (rtt != null) {
                samples.add(rtt)
            } else {
                lostPackets++
            }
            // Strict 10ms rest between bursts to prevent triggering false congestion
            delay(10)
        }

        if (samples.isEmpty()) {
            return@withContext LatencyMetrics(0f, 0f, 0, 0, 0f, 100f, emptyList())
        }

        samples.sort()
        val min = samples.first()
        val max = samples.last()
        val mean = samples.average().toFloat()
        val median = samples[samples.size / 2].toFloat()

        // Calculate Jitter (Average absolute deviation of consecutive samples)
        var jitterSum = 0L
        var jitterCount = 0
        for (i in 1 until samples.size) {
            jitterSum += abs(samples[i] - samples[i - 1])
            jitterCount++
        }
        val jitter = if (jitterCount > 0) (jitterSum.toFloat() / jitterCount) else 0f

        val packetLoss = (lostPackets.toFloat() / burstCount) * 100f

        LatencyMetrics(
            meanRtt = mean,
            medianRtt = median,
            minRtt = min,
            maxRtt = max,
            jitter = jitter,
            packetLossPercent = packetLoss,
            rawSamples = samples
        )
    }

    /**
     * Bypasses the need for Rooted ICMP pings by executing a rapid TCP Handshake.
     * The exact delta between the SYN and SYN-ACK serves as a highly precise RTT measurement.
     */
    private fun measureTcpPing(ipAddress: String, port: Int, timeoutMs: Int): Long? {
        var socket: Socket? = null
        val start = System.nanoTime()
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(ipAddress, port), timeoutMs)
            val end = System.nanoTime()
            (end - start) / 1_000_000 // Convert nanoseconds to milliseconds
        } catch (e: Exception) {
            null // Timeout or rejection equals a dropped packet
        } finally {
            try { socket?.close() } catch (e: Exception) {}
        }
    }
}
