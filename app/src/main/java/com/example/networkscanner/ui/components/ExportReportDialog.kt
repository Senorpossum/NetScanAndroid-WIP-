package com.example.networkscanner.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.networkscanner.domain.model.NetworkSnapshot
import com.example.networkscanner.domain.service.AuditPdfExporter
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportDialog(
    snapshot: NetworkSnapshot,
    topologyBitmap: Bitmap?,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Export Security Audit", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Generate a cryptographically sealed PDF report or export raw JSON for external tool ingestion.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (isGenerating) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Rendering Multi-Page PDF...")
            } else {
                Button(
                    onClick = {
                        isGenerating = true
                        coroutineScope.launch {
                            val exporter = AuditPdfExporter(context)
                            // Offload heavy PDF layout rendering to the background
                            val pdfFile = exporter.generateReport(snapshot, topologyBitmap)
                            
                            if (pdfFile != null) {
                                dispatchSharesheet(context, pdfFile)
                            }
                            
                            isGenerating = false
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export as Sealed PDF")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        // Scaffold: Generate JSON String and dispatch as text/plain
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Raw JSON")
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

private fun dispatchSharesheet(context: Context, file: File) {
    // Generate secure Content URI via the Android FileProvider
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Network Security Audit Report")
        putExtra(Intent.EXTRA_TEXT, "Attached is the cryptographically sealed Network Audit Report.")
        // Crucial flag allowing the receiving app (e.g. Gmail) to briefly read our cache file
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Share Audit Report"))
}
