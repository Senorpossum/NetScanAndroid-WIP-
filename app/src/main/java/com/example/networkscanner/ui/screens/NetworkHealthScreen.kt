package com.example.networkscanner.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.networkscanner.domain.service.BufferbloatResult
import com.example.networkscanner.domain.service.LatencyMetrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkHealthScreen(
    viewModel: com.example.networkscanner.ui.viewmodel.NetworkHealthViewModel,
    idleMetrics: LatencyMetrics?,
    bufferbloatResult: BufferbloatResult?,
    isTesting: Boolean,
    onRunLoadTest: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        viewModel.startMonitoring()
        onDispose {
            viewModel.stopMonitoring()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Health") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Live QoS Oscilloscope",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Oscilloscope Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black.copy(alpha = 0.8f))
            ) {
                val samples = idleMetrics?.rawSamples ?: emptyList()
                if (samples.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxVal = (samples.maxOrNull() ?: 100L).coerceAtLeast(10L).toFloat()
                        val widthPerSample = size.width / samples.size.coerceAtLeast(1)
                        
                        val path = Path().apply {
                            samples.forEachIndexed { index, sample ->
                                val x = index * widthPerSample
                                val normalizedY = 1f - (sample.toFloat() / maxVal)
                                val y = normalizedY * size.height
                                
                                if (index == 0) moveTo(x, y) else lineTo(x, y)
                            }
                        }
                        
                        drawPath(
                            path = path,
                            color = Color.Cyan,
                            style = Stroke(width = 3f)
                        )
                    }
                } else {
                    Text(
                        text = if (isTesting) "Load Test in Progress..." else "Awaiting Latency Samples...",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricGauge("Jitter", "${String.format("%.1f", idleMetrics?.jitter ?: 0f)} ms")
                MetricGauge("Packet Loss", "${String.format("%.1f", idleMetrics?.packetLossPercent ?: 0f)}%")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bufferbloat Grade Indicator
            if (bufferbloatResult != null && !isTesting) {
                Text(
                    text = "Bufferbloat Grade",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = bufferbloatResult.grade,
                    style = MaterialTheme.typography.displayLarge,
                    color = when (bufferbloatResult.grade) {
                        "A+", "A" -> Color.Green
                        "B", "C" -> Color.Yellow
                        else -> Color.Red
                    }
                )
                Text(
                    text = "+${String.format("%.1f", bufferbloatResult.inflationDelta)} ms under load",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRunLoadTest, modifier = Modifier.fillMaxWidth()) {
                    Text("Re-run Load Test")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bufferbloat Test", style = MaterialTheme.typography.titleMedium)
                        Text("Test latency while saturating the link.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRunLoadTest,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isTesting
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Run Load Test")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricGauge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
}
