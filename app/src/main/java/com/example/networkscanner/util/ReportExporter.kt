package com.example.networkscanner.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.networkscanner.ui.viewmodel.HostWithPorts
import com.example.networkscanner.data.repository.WifiScanAudit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object ReportExporter {

    suspend fun exportLanScanToMarkdown(hosts: List<HostWithPorts>): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        sb.append("# LAN Audit Report\n\n")
        sb.append("Total Hosts Discovered: ${hosts.size}\n\n")
        
        hosts.forEach { host ->
            sb.append("### Host: ${host.host.ipAddress}\n")
            sb.append("- **Hostname:** ${host.host.hostname ?: "Unknown"}\n")
            sb.append("- **MAC:** ${host.host.macAddress ?: "Unknown"}\n")
            if (host.openPorts.isNotEmpty()) {
                sb.append("- **Open Ports:**\n")
                host.openPorts.forEach { port ->
                    sb.append("  - ${port.port}/tcp (${port.serviceName}) - Severity: ${port.riskSeverity.name}\n")
                }
            } else {
                sb.append("- **Open Ports:** None detected\n")
            }
            sb.append("\n")
        }
        sb.toString()
    }

    suspend fun exportWifiAuditToJson(audits: List<WifiScanAudit>): String = withContext(Dispatchers.Default) {
        val root = JSONArray()
        audits.forEach { audit ->
            val obj = JSONObject().apply {
                put("ssid", audit.ssid)
                put("bssid", audit.bssid)
                put("capabilities", audit.capabilities)
                put("frequency", audit.frequency)
                put("rssi", audit.rssi)
                put("isPotentialRogue", audit.isPotentialRogue)
            }
            root.put(obj)
        }
        root.toString(4)
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
