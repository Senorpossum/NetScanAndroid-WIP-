package com.example.networkscanner.domain.model

/**
 * Defines execution configurations for network sweeps.
 * Adjusting these profiles allows the scanner to balance between speed and stealth.
 */
enum class ScanProfile(
    val title: String,
    val description: String,
    val maxThreads: Int,
    val targetPorts: List<Int>,
    val connectionTimeoutMs: Int,
    val interProbeDelayMs: Long
) {
    STEALTH(
        title = "Stealth (Low Impact)",
        description = "Slower intervals and strict 2-thread limit to evade simple IDS algorithms and network Firewalls.",
        maxThreads = 2,
        targetPorts = listOf(22, 80, 443, 8080),
        connectionTimeoutMs = 1500, // Longer timeout for sluggish responses
        interProbeDelayMs = 250L    // Explicit delay between packets
    ),
    STANDARD(
        title = "Standard (Balanced)",
        description = "Balanced 64-thread concurrent sweep covering common management ports.",
        maxThreads = 64,
        targetPorts = listOf(21, 22, 23, 53, 80, 443, 5555, 8080, 8443, 9443),
        connectionTimeoutMs = 500,
        interProbeDelayMs = 10L
    ),
    DEEP_AUDIT(
        title = "Deep Audit",
        description = "Aggressive full-throttle execution across the top 1000 most common ports with active banner grabbing.",
        maxThreads = 128,
        targetPorts = (1..1000).toList(), // In production, this maps to Nmap's top-1000 list
        connectionTimeoutMs = 300,
        interProbeDelayMs = 0L // No delay, maximize OS socket saturation
    )
}
