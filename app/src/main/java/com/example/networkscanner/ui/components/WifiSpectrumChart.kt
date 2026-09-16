package com.example.networkscanner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.networkscanner.data.repository.WifiScanAudit

@Composable
fun WifiSpectrumChart(
    audits: List<WifiScanAudit>,
    is5Ghz: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceVariant)
    ) {
        val width = size.width
        val height = size.height

        // Define Band Boundaries
        val minFreq = if (is5Ghz) 5180 else 2412
        val maxFreq = if (is5Ghz) 5825 else 2484
        val freqRange = maxFreq - minFreq

        val padding = 50f
        val chartWidth = width - (padding * 2)
        val chartHeight = height - (padding * 2)

        // Draw X/Y Axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 3f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 3f
        )

        // Draw Channel Labels
        if (is5Ghz) {
            listOf(36, 48, 149, 161).forEach { channel ->
                val freq = 5000 + (channel * 5)
                val x = padding + (chartWidth * (freq - minFreq).toFloat() / freqRange)
                if (x in padding..width - padding) {
                    drawText(textMeasurer, channel.toString(), Offset(x - 10f, height - padding + 10f), style = labelStyle)
                }
            }
        } else {
            (1..13).forEach { channel ->
                val freq = 2412 + (channel - 1) * 5
                val x = padding + (chartWidth * (freq - minFreq).toFloat() / freqRange)
                drawText(textMeasurer, channel.toString(), Offset(x - 10f, height - padding + 10f), style = labelStyle)
            }
        }

        val filteredAudits = audits.filter { 
            if (is5Ghz) it.frequency >= 5000 else it.frequency < 5000 
        }

        // Render Parabolic Overlap Curves
        filteredAudits.forEach { audit ->
            
            // 1. Map RSSI (-100 to -30 dBm) to Y-Axis Amplitude
            val normalizedRssi = (audit.rssi + 100).coerceIn(0, 70) / 70f
            val peakY = (height - padding) - (chartHeight * normalizedRssi)
            
            // 2. Map Center Frequency to X-Axis Position
            val freqOffset = (audit.frequency - minFreq).toFloat() / freqRange
            val peakX = padding + (chartWidth * freqOffset)
            
            // 3. Define 20MHz Bandwidth Spread
            val widthPerMhz = chartWidth / freqRange
            val spreadX = 10f * widthPerMhz // ±10MHz from center
            
            val startX = peakX - spreadX
            val endX = peakX + spreadX
            val baseY = height - padding
            
            val path = Path().apply {
                moveTo(startX, baseY)
                // Construct a smooth bell curve using a Quadratic Bezier
                quadraticBezierTo(
                    x1 = peakX, 
                    y1 = peakY - (chartHeight * 0.2f), // Control point apex tension
                    x2 = endX, 
                    y2 = baseY
                )
            }
            
            val curveColor = if (audit.isPotentialRogue) Color.Red else Color.Cyan
            
            // Render Translucent Area Fill
            drawPath(
                path = path,
                color = curveColor.copy(alpha = 0.2f)
            )
            // Render Hard Stroke Outline
            drawPath(
                path = path,
                color = curveColor,
                style = Stroke(width = 4f)
            )
            
            // Draw SSID Label
            drawText(
                textMeasurer = textMeasurer,
                text = audit.ssid.take(12),
                topLeft = Offset(peakX - 30f, peakY - 40f),
                style = labelStyle.copy(color = onSurfaceVariant)
            )
        }
    }
}
