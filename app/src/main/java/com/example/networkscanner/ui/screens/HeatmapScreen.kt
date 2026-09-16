package com.example.networkscanner.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import com.example.networkscanner.domain.model.SurveyPoint
import com.example.networkscanner.util.HeatmapInterpolationEngine
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun HeatmapScreen(
    surveyPoints: List<SurveyPoint>,
    onSampleDropped: (Offset) -> Unit,
    onSampleDeleted: (SurveyPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var heatmapBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    // Recompute IDW heatmap asynchronously when physical sample points update
    LaunchedEffect(surveyPoints, canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val bitmap = HeatmapInterpolationEngine.generateHeatmapBitmap(
                points = surveyPoints,
                canvasWidth = canvasSize.width.toFloat(),
                canvasHeight = canvasSize.height.toFloat(),
                matrixResolution = 80 // Hard downscale for 60fps non-blocking rendering
            )
            heatmapBitmap = bitmap?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.DarkGray) // Base Scaffold Color (represents floorplan image)
            // 1. Gesture Tracking: Infinite Zoom and Pan
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    scale = (scale * zoomChange).coerceIn(0.5f, 5f)
                    pan += panChange
                }
            }
            // 2. Interactive Survey Tap Drops
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val adjustedTap = Offset(
                            x = (tapOffset.x - pan.x) / scale,
                            y = (tapOffset.y - pan.y) / scale
                        )

                        // Check if tapping existing point to delete
                        val clickedNode = surveyPoints.find { node ->
                            val distance = sqrt(
                                (adjustedTap.x - node.x).pow(2) + 
                                (adjustedTap.y - node.y).pow(2)
                            )
                            distance <= 30f // Touch slop forgiveness
                        }

                        if (clickedNode != null) {
                            onSampleDeleted(clickedNode)
                        } else {
                            // Drop new physical RF sample
                            onSampleDropped(adjustedTap)
                        }
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = pan.x,
                    translationY = pan.y
                )
        ) {
            canvasSize = IntSize(size.width.toInt(), size.height.toInt())
            
            // Layer 1: Floorplan Image (Scaffolded as a schematic grid)
            val gridSize = 100f
            var xPos = 0f
            while (xPos < size.width) {
                drawLine(Color.Gray.copy(alpha = 0.3f), Offset(xPos, 0f), Offset(xPos, size.height))
                xPos += gridSize
            }
            var yPos = 0f
            while (yPos < size.height) {
                drawLine(Color.Gray.copy(alpha = 0.3f), Offset(0f, yPos), Offset(size.width, yPos))
                yPos += gridSize
            }

            // Layer 2: Generated Inverse Distance Weighting Bitmap Matrix
            heatmapBitmap?.let { bmp ->
                drawImage(
                    image = bmp,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = FilterQuality.High // Force hardware-accelerated bilinear upscaling
                )
            }

            // Layer 3: Interactive Physical Sample Nodes
            surveyPoints.forEach { point ->
                val center = Offset(point.x, point.y)
                val color = when {
                    point.rssi >= -55 -> Color.Green
                    point.rssi in -70..-56 -> Color.Yellow
                    else -> Color.Red
                }
                
                drawCircle(
                    color = color,
                    radius = 15f,
                    center = center
                )
                // Render white stroke for contrast against gradient
                drawCircle(
                    color = Color.White,
                    radius = 15f,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}
