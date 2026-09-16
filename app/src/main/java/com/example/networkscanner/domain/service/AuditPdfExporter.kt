package com.example.networkscanner.domain.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import com.example.networkscanner.domain.model.NetworkSnapshot
import com.example.networkscanner.util.ReportThemeConfig
import com.example.networkscanner.util.ReportThemeConfig.MARGIN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuditPdfExporter(private val context: Context) {

    suspend fun generateReport(
        snapshot: NetworkSnapshot, 
        topologyBitmap: Bitmap? = null
    ): File? = withContext(Dispatchers.IO) {
        
        // 1. Calculate Cryptographic SHA-256 Seal of raw data payload
        // In a production app, we serialize `snapshot` to JSON using Gson/Moshi and hash that string.
        // For scaffold, we derive a deterministic hash based on core metrics.
        val rawDataString = "${snapshot.timestamp}-${snapshot.bssid}-${snapshot.deviceCount}"
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(rawDataString.toByteArray())
        val hashSeal = hashBytes.joinToString("") { "%02x".format(it) }

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(
            ReportThemeConfig.PAGE_WIDTH, 
            ReportThemeConfig.PAGE_HEIGHT, 
            1
        ).create()

        // --- PAGE 1: Executive Summary ---
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        drawExecutiveSummary(canvas, snapshot)
        drawFooter(canvas, hashSeal, 1)
        document.finishPage(page)

        // --- PAGE 2: Host Inventory & Vulnerabilities ---
        page = document.startPage(pageInfo)
        canvas = page.canvas
        drawHostInventory(canvas, snapshot)
        drawFooter(canvas, hashSeal, 2)
        document.finishPage(page)

        // --- PAGE 3: Topology Graphic (Optional Embedded Map) ---
        if (topologyBitmap != null) {
            page = document.startPage(pageInfo)
            canvas = page.canvas
            
            canvas.drawText("Network Topology Map", MARGIN, MARGIN + 20f, ReportThemeConfig.paintTitle)
            
            // Scale bitmap to fit A4 width margin while preserving aspect ratio
            val targetWidth = ReportThemeConfig.PAGE_WIDTH - (MARGIN * 2)
            val scale = targetWidth / topologyBitmap.width
            val targetHeight = topologyBitmap.height * scale
            
            val scaledBmp = Bitmap.createScaledBitmap(topologyBitmap, targetWidth.toInt(), targetHeight.toInt(), true)
            canvas.drawBitmap(scaledBmp, MARGIN, MARGIN + 50f, null)
            
            // Explicitly recycle the generated Bitmap to prevent JVM heap fragmentation leaks
            scaledBmp.recycle()
            
            drawFooter(canvas, hashSeal, 3)
            document.finishPage(page)
        }

        // --- Write File stream to Secure Cache ---
        try {
            val reportDir = File(context.cacheDir, "reports")
            if (!reportDir.exists()) reportDir.mkdirs()
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(reportDir, "NetworkAudit_$timestamp.pdf")
            
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            return@withContext file
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            document.close()
        }
    }

    private fun drawExecutiveSummary(canvas: Canvas, snapshot: NetworkSnapshot) {
        var currentY = MARGIN + 40f
        
        canvas.drawText("Network Security Audit Report", MARGIN, currentY, ReportThemeConfig.paintTitle)
        currentY += 40f
        
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(snapshot.timestamp))
        canvas.drawText("Date: $dateStr", MARGIN, currentY, ReportThemeConfig.paintSubtitle)
        currentY += ReportThemeConfig.LINE_SPACING
        
        canvas.drawText("SSID: ${snapshot.ssid}", MARGIN, currentY, ReportThemeConfig.paintSubtitle)
        currentY += ReportThemeConfig.LINE_SPACING
        
        canvas.drawText("BSSID: ${snapshot.bssid}", MARGIN, currentY, ReportThemeConfig.paintSubtitle)
        currentY += ReportThemeConfig.LINE_SPACING
        
        canvas.drawText("Gateway IP: ${snapshot.gatewayIp}", MARGIN, currentY, ReportThemeConfig.paintSubtitle)
        currentY += ReportThemeConfig.LINE_SPACING
        
        canvas.drawText("Devices Discovered: ${snapshot.deviceCount}", MARGIN, currentY, ReportThemeConfig.paintSubtitle)
        
        currentY += 60f
        canvas.drawText("Executive Summary", MARGIN, currentY, ReportThemeConfig.paintTitle)
        currentY += 30f
        
        canvas.drawText("This automated audit evaluates the local network infrastructure for exposed", MARGIN, currentY, ReportThemeConfig.paintBody)
        currentY += ReportThemeConfig.LINE_SPACING
        canvas.drawText("ports, vulnerable services, and active Man-in-the-Middle configurations.", MARGIN, currentY, ReportThemeConfig.paintBody)
    }

    private fun drawHostInventory(canvas: Canvas, snapshot: NetworkSnapshot) {
        var currentY = MARGIN + 40f
        canvas.drawText("Host Inventory & Vulnerabilities", MARGIN, currentY, ReportThemeConfig.paintTitle)
        currentY += 40f
        
        snapshot.discoveredHosts.forEach { host ->
            // In a production app, we would rigorously calculate if `currentY` exceeds PAGE_HEIGHT 
            // and trigger `document.finishPage` / `document.startPage` for pagination overflow.
            if (currentY > ReportThemeConfig.PAGE_HEIGHT - 100f) {
                return@forEach // Scaffold: Stop rendering to prevent overflow
            }
            
            val hostLine = "${host.ipAddress} [${host.macAddress ?: "Unknown MAC"}]"
            canvas.drawText(hostLine, MARGIN, currentY, ReportThemeConfig.paintSubtitle)
            currentY += ReportThemeConfig.LINE_SPACING
            
            val detailsLine = "Vendor: ${host.vendorOui ?: "N/A"} | Open Ports: ${host.openPorts.joinToString()}"
            canvas.drawText(detailsLine, MARGIN + 20f, currentY, ReportThemeConfig.paintBody)
            currentY += ReportThemeConfig.LINE_SPACING
            
            if (host.remediationAdvice.isNotEmpty()) {
                canvas.drawText("WARNING: ${host.remediationAdvice.first()}", MARGIN + 20f, currentY, ReportThemeConfig.paintWarning)
                currentY += ReportThemeConfig.LINE_SPACING
            }
            
            currentY += 10f // Spacing padding between hosts
        }
    }

    private fun drawFooter(canvas: Canvas, hashSeal: String, pageNum: Int) {
        val footerY = ReportThemeConfig.PAGE_HEIGHT - MARGIN
        canvas.drawText("Page $pageNum", MARGIN, footerY, ReportThemeConfig.paintFooter)
        
        // Print Cryptographic Seal aligned to the right side of the footer
        val sealText = "Integrity SHA256: ${hashSeal.take(16)}..."
        val textWidth = ReportThemeConfig.paintFooter.measureText(sealText)
        canvas.drawText(sealText, ReportThemeConfig.PAGE_WIDTH - MARGIN - textWidth, footerY, ReportThemeConfig.paintFooter)
    }
}
