package com.example.networkscanner.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class BufferbloatResult(
    val idleLatency: Float,
    val loadedLatency: Float,
    val inflationDelta: Float,
    val grade: String
)

class BufferbloatAuditor(private val profiler: LatencyProfiler) {

    private val loadDispatcher = Dispatchers.IO.limitedParallelism(16)

    suspend fun runAudit(gatewayIp: String): BufferbloatResult = withContext(Dispatchers.Default) {
        // 1. Establish Idle Baseline
        val idleMetrics = profiler.profileTarget(gatewayIp, burstCount = 20)
        val idleLatency = idleMetrics.meanRtt

        var loadedLatency = 0f
        
        // 2. Introduce Controlled Active Load
        coroutineScope {
            // Spawn background stress jobs with limited parallelism to prevent socket exhaustion
            val loadJob = launch(loadDispatcher) {
                simulateNetworkLoad(gatewayIp)
            }
            
            // Allow buffers to spool
            delay(500)
            
            // Measure latency UNDER load
            val loadedMetrics = profiler.profileTarget(gatewayIp, burstCount = 30)
            loadedLatency = loadedMetrics.meanRtt
            
            // Gracefully stop the load
            loadJob.cancel()
        }

        val inflation = (loadedLatency - idleLatency).coerceAtLeast(0f)

        // 3. Assign Bufferbloat Grade
        val grade = when {
            inflation < 8f -> "A+"
            inflation < 20f -> "A"
            inflation < 45f -> "B"
            inflation < 80f -> "C"
            else -> "F"
        }

        BufferbloatResult(
            idleLatency = idleLatency,
            loadedLatency = loadedLatency,
            inflationDelta = inflation,
            grade = grade
        )
    }

    private suspend fun simulateNetworkLoad(targetIp: String) = coroutineScope {
        // Use fewer concurrent connections but keep them active slightly longer 
        // to saturate buffers without pinning the OS socket table.
        val parallelConnections = 8
        val jobs = (1..parallelConnections).map {
            launch(loadDispatcher) {
                while (true) {
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(targetIp, 80), 1000)
                            // Artificial delay to prevent CPU spinning
                            delay(50) 
                        }
                    } catch (e: Exception) {
                        delay(100) // Backoff on error
                    }
                }
            }
        }
    }
}
