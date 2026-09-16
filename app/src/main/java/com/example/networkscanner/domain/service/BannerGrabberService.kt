package com.example.networkscanner.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

class BannerGrabberService {

    suspend fun grabBanner(ipAddress: String, port: Int): String? = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            // Apply a strict 300ms read timeout and 300ms connect timeout to prevent blocking sweeps
            socket = Socket()
            socket.connect(InetSocketAddress(ipAddress, port), 300)
            socket.soTimeout = 300

            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            // Actively probe for HTTP server headers
            if (port == 80 || port == 8080 || port == 443) {
                sendHttpProbe(outputStream)
            }

            readBanner(inputStream)
        } catch (e: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }

    private fun sendHttpProbe(outputStream: OutputStream) {
        val probe = "HEAD / HTTP/1.0\r\n\r\n"
        outputStream.write(probe.toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }

    private fun readBanner(inputStream: InputStream): String? {
        val buffer = ByteArray(512)
        val bytesRead = inputStream.read(buffer)
        if (bytesRead > 0) {
            val response = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
            return parseBanner(response)
        }
        return null
    }

    private fun parseBanner(response: String): String {
        if (response.startsWith("HTTP/")) {
            val lines = response.split("\r\n", "\n")
            for (line in lines) {
                if (line.startsWith("Server:", ignoreCase = true)) {
                    return line.substringAfter(":").trim()
                }
            }
            return lines.firstOrNull() ?: response.take(100)
        }
        return response.trim().take(100)
    }
}
