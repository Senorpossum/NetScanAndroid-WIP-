package com.example.networkscanner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.example.networkscanner.ui.viewmodel.HostWithPorts
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Holds the mapped screen coordinates for hit detection
data class NodePosition(val host: HostWithPorts, val center: Offset, val radius: Float)

@Composable
fun NetworkTopologyGraph(
    hosts: List<HostWithPorts>,
    onNodeTap: (HostWithPorts) -> Unit,
    modifier: Modifier = Modifier
) {
    // Transformation Matrices for Gestures
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var nodePositions by remember { mutableStateOf<List<NodePosition>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // 1. Pinch to Zoom and Pan
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    scale = (scale * zoomChange).coerceIn(0.5f, 5f)
                    pan += panChange
                }
            }
            // 2. Hit Detection mapped against the inverse matrix
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        // Reverse calculate the tap offset based on current scale and pan translation
                        val adjustedTap = Offset(
                            x = (tapOffset.x - pan.x) / scale,
                            y = (tapOffset.y - pan.y) / scale
                        )
                        
                        val clickedNode = nodePositions.find { node ->
                            val distance = sqrt(
                                (adjustedTap.x - node.center.x).pow(2) + 
                                (adjustedTap.y - node.center.y).pow(2)
                            )
                            distance <= node.radius + 30f // Expanded touch slop target
                        }
                        
                        clickedNode?.let { onNodeTap(it.host) }
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
            val center = Offset(size.width / 2, size.height / 2)
            val ringRadius = size.minDimension / 3
            
            // Draw Subnet Orbital Ring
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2f)
            )
            
            // Draw Core Gateway Node (0,0 relative)
            drawCircle(
                color = Color.Green,
                radius = 35f,
                center = center
            )

            val newPositions = mutableListOf<NodePosition>()
            val hostCount = hosts.size
            val angleStep = (2 * Math.PI) / (hostCount.coerceAtLeast(1))

            // Radially distribute hosts via Trigonometry
            hosts.forEachIndexed { index, hostInfo ->
                val angle = index * angleStep
                val x = center.x + (ringRadius * cos(angle)).toFloat()
                val y = center.y + (ringRadius * sin(angle)).toFloat()
                
                val nodeCenter = Offset(x, y)
                val nodeColor = if (hostInfo.openPorts.isNotEmpty()) Color.Red else Color.Cyan
                
                // Spoke / Link
                drawLine(
                    color = Color.Gray.copy(alpha = 0.4f),
                    start = center,
                    end = nodeCenter,
                    strokeWidth = 3f
                )
                
                // Host Node
                drawCircle(
                    color = nodeColor,
                    radius = 25f,
                    center = nodeCenter
                )
                
                newPositions.add(NodePosition(hostInfo, nodeCenter, 25f))
            }
            
            nodePositions = newPositions
        }
    }
}
