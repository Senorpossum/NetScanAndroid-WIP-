package com.example.networkscanner.domain.model

data class NetworkSnapshot(
    val id: Long = 0,
    val timestamp: Long,
    val ssid: String,
    val bssid: String,
    val gatewayIp: String,
    val subnetMask: String,
    val deviceCount: Int,
    val discoveredHosts: List<DiscoveredHost>
)

data class DiscoveredHost(
    val id: Long = 0,
    val ipAddress: String,
    val macAddress: String?,
    val vendorOui: String?,
    val hostname: String?,
    val openPorts: List<Int>,
    val isNewDevice: Boolean,
    val bannerString: String? = null,
    val mdnsHostname: String? = null,
    val ssdpManufacturer: String? = null,
    val ssdpModelName: String? = null,
    val ssdpModelNumber: String? = null,
    val ssdpSerialNumber: String? = null,
    // Vulnerability Engine outputs
    val securityScore: Int = 100,
    val remediationAdvice: List<String> = emptyList(),
    // Transport Security
    val tlsAuditResult: TlsAuditResult? = null,
    val dnsSecurityReport: DnsSecurityReport? = null
)

data class TlsAuditResult(
    val subjectDn: String,
    val issuerDn: String,
    val protocol: String,
    val cipherSuite: String,
    val isExpired: Boolean,
    val isExpiringSoon: Boolean,
    val hasObsoleteSignature: Boolean,
    val isSelfSigned: Boolean,
    val warnings: List<String>
)

data class DnsSecurityReport(
    val activeDnsServers: List<String>,
    val isPrivateDnsActive: Boolean,
    val isHijacked: Boolean,
    val warnings: List<String>
)

data class SurveyPoint(
    val id: Long = 0,
    val floorplanId: Long,
    val x: Float,
    val y: Float,
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val linkSpeed: Int
)
