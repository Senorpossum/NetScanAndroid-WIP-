package com.example.networkscanner.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.networkscanner.ui.viewmodel.SignalTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalTrackerScreen(
    viewModel: SignalTrackerViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")

    // Start tracking when the screen enters composition, stop when it leaves
    DisposableEffect(Unit) {
        viewModel.startTracking()
        onDispose {
            viewModel.stopTracking()
        }
    }
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Precision Signal Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SSID: ${state.ssid}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = state.bssid,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Radar Visualizer
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                RadarCircle(state.signalStrengthPercentage, pulseAlpha, getSignalColor(state.rssi))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${String.format("%.1f", state.smoothedRssi)}",
                        style = MaterialTheme.typography.displayMedium,
                        color = getSignalColor(state.rssi)
                    )
                    Text(
                        text = "dBm",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Signal History Sparkline
            SignalHistoryChart(state.history)

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = getSignalColor(state.rssi).copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Real-time Proximity",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = getProximityAdvice(state.rssi),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun RadarCircle(percentage: Int, pulseAlpha: Float, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2
        
        // Static rings
        for (i in 1..4) {
            drawCircle(
                color = Color.Gray.copy(alpha = 0.1f),
                radius = maxRadius * (i / 4f),
                center = center,
                style = Stroke(width = 1f)
            )
        }

        // Active signal ring
        val activeRadius = maxRadius * (percentage / 100f).coerceAtLeast(0.05f)
        drawCircle(
            color = color.copy(alpha = pulseAlpha),
            radius = activeRadius,
            center = center,
            style = Stroke(width = 12f)
        )
        
        drawCircle(
            color = color.copy(alpha = 0.05f),
            radius = activeRadius,
            center = center
        )
    }
}

@Composable
fun SignalHistoryChart(history: List<Double>) {
    Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color.Black.copy(alpha = 0.05f))) {
        if (history.size > 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    val widthPerPoint = size.width / (history.size - 1)
                    val min = -100f
                    val max = -30f
                    val range = max - min

                    history.forEachIndexed { index, value ->
                        val x = index * widthPerPoint
                        val normalizedY = 1f - ((value.toFloat() - min) / range).coerceIn(0f, 1f)
                        val y = normalizedY * size.height
                        
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(path, Color.Cyan, style = Stroke(width = 2.dp.toPx()))
            }
        } else {
            Text("Collecting history...", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
        }
    }
}

fun getSignalColor(rssi: Int): Color {
    return when {
        rssi >= -50 -> Color(0xFF4CAF50) // Green
        rssi >= -65 -> Color(0xFF8BC34A) // Light Green
        rssi >= -75 -> Color(0xFFFFC107) // Amber
        rssi >= -85 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

fun getProximityAdvice(rssi: Int): String {
    return when {
        rssi >= -40 -> "Source is right here! (Direct Line of Sight)"
        rssi >= -55 -> "Very close. Move slowly to pinpoint the exact location."
        rssi >= -70 -> "Signal detected. Following the trend..."
        rssi >= -85 -> "Distant source. Look for doorways or open spaces."
        else -> "Searching for signal. Try moving to a different room."
    }
}
