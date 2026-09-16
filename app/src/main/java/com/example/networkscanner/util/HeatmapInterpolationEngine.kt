package com.example.networkscanner.util

import android.graphics.Bitmap
import android.graphics.Color
import com.example.networkscanner.domain.model.SurveyPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

object HeatmapInterpolationEngine {

    /**
     * Calculates the IDW heatmap and returns an Android Bitmap.
     * To ensure 60fps UI performance, the heavy mathematics are executed against a 
     * severely downscaled logical matrix (e.g. 100x100) on a background thread.
     * The resulting Bitmap is then rendered natively in Compose using FilterQuality.High 
     * to smoothly upscale the gradient back to the device's physical resolution.
     */
    suspend fun generateHeatmapBitmap(
        points: List<SurveyPoint>, 
        canvasWidth: Float, 
        canvasHeight: Float,
        matrixResolution: Int = 100
    ): Bitmap? = withContext(Dispatchers.Default) {
        if (points.isEmpty() || canvasWidth <= 0 || canvasHeight <= 0) return@withContext null

        val width = matrixResolution
        val height = (matrixResolution * (canvasHeight / canvasWidth)).toInt().coerceAtLeast(1)

        val pixels = IntArray(width * height)
        val power = 2.0 // IDW Power

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Map the logical matrix grid coordinates back to physical canvas coordinates
                val physicalX = (x.toFloat() / width) * canvasWidth
                val physicalY = (y.toFloat() / height) * canvasHeight

                var weightedSum = 0.0
                var weightSum = 0.0
                var exactMatch = false
                var exactValue = 0

                for (p in points) {
                    val distance = sqrt((physicalX - p.x).pow(2f) + (physicalY - p.y).pow(2f))
                    if (distance < 1.0f) { // Point is exactly here
                        exactMatch = true
                        exactValue = p.rssi
                        break
                    }
                    val weight = 1.0 / distance.toDouble().pow(power)
                    weightedSum += p.rssi * weight
                    weightSum += weight
                }

                val interpolatedRssi = if (exactMatch) exactValue else (weightedSum / weightSum).toInt()
                
                // Convert Interpolated RSSI to Matrix Color
                val color = mapRssiToColor(interpolatedRssi)
                pixels[y * width + x] = color
            }
        }

        return@withContext Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun mapRssiToColor(rssi: Int): Int {
        return when {
            rssi >= -55 -> Color.argb(150, 0, 255, 0)       // Strong (Green)
            rssi in -70..-56 -> Color.argb(150, 255, 200, 0) // Moderate (Amber)
            rssi in -85..-71 -> Color.argb(150, 255, 0, 0)   // Weak (Red)
            else -> Color.argb(150, 128, 0, 128)            // Dead Zone (Purple)
        }
    }
}
