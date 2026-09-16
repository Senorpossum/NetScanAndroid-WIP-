package com.example.networkscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.networkscanner.domain.model.DiscoveredHost
import com.example.networkscanner.domain.service.CustomSocketProbe
import com.example.networkscanner.domain.service.WakeOnLanService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostActionBottomSheet(
    host: DiscoveredHost,
    onDismissRequest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var rawPayload by remember { mutableStateOf("GET / HTTP/1.1\\r\\n\\r\\n") }
    var targetPort by remember { mutableStateOf("80") }
    var probeResponse by remember { mutableStateOf<String?>(null) }
    var isProbing by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Host Intervention Tools", style = MaterialTheme.typography.headlineSmall)
            Text(host.ipAddress, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(24.dp))

            // Wake on LAN Action
            Button(
                onClick = {
                    host.macAddress?.let { mac ->
                        coroutineScope.launch {
                            val wolService = WakeOnLanService()
                            // Dispatch standard WoL Magic Packet over UDP 255.255.255.255
                            wolService.sendMagicPacket(mac)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = host.macAddress != null
            ) {
                Text(if (host.macAddress != null) "Send Wake-on-LAN (WoL) Packet" else "MAC Address Required for WoL")
            }

            Spacer(modifier = Modifier.height(32.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Interactive Socket Probe", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = targetPort,
                onValueChange = { targetPort = it.filter { char -> char.isDigit() } },
                label = { Text("Target TCP Port") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = rawPayload,
                onValueChange = { rawPayload = it },
                label = { Text("Raw Payload String (Accepts \\r\\n formatting)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isProbing = true
                    coroutineScope.launch {
                        val probe = CustomSocketProbe()
                        // Interactively writes the raw payload string directly into the socket stream
                        probeResponse = probe.executeTcpProbe(
                            ipAddress = host.ipAddress,
                            port = targetPort.toIntOrNull() ?: 80,
                            payload = rawPayload
                        )
                        isProbing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (isProbing) "Probing Socket..." else "Fire Raw TCP Payload")
            }
            
            // Response Terminal Visualizer
            if (probeResponse != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = probeResponse!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
