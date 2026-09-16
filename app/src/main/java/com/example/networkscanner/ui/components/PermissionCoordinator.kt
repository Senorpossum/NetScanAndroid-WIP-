package com.example.networkscanner.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionCoordinator(
    onPermissionsGranted: @Composable () -> Unit
) {
    val permissions = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissions)

    // Log the current permission state for debugging
    LaunchedEffect(permissionState.permissions) {
        permissionState.permissions.forEach { perm ->
            android.util.Log.d("PermissionCoord", "Permission: ${perm.permission}, Granted: ${perm.status.isGranted}")
        }
    }

    // Essential permissions for the app to function at all
    val essentialPermissionsGranted = permissionState.permissions
        .filter { 
            // Notifications are optional for the core scanning functionality
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.permission != Manifest.permission.POST_NOTIFICATIONS
            } else {
                true
            }
        }
        .all { it.status.isGranted }

    if (essentialPermissionsGranted) {
        onPermissionsGranted()
    } else {
        PermissionPreFlightSheet(
            onRequestPermission = { permissionState.launchMultiplePermissionRequest() }
        )
    }
}

@Composable
fun PermissionPreFlightSheet(onRequestPermission: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Network Auditor",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "To perform deep packet inspections and Wi-Fi recon, the app requires specific permissions:",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PermissionJustificationItem(
                title = "Precise Location & Wi-Fi",
                body = "Android requires Location access to read the physical MAC address and SSID of nearby Wi-Fi routers. This data is never sent to the cloud."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PermissionJustificationItem(
                title = "Notifications",
                body = "Required to run the Network Sentinel in the background and alert you of active ARP spoofing (MitM) attacks."
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Grant Permissions", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun PermissionJustificationItem(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
