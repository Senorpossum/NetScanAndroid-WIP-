package com.example.networkscanner.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

class CustomSocketProbe {

    /**
     * Opens a TCP socket to the target, writes the custom raw payload, and blocks briefly 
     * to capture and return the server's immediate ASCII response string.
     */
    suspend fun executeTcpProbe(
        ipAddress: String,
        port: Int,
        payload: String,
        timeoutMs: Int = 3000
    ): String = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            // Strictly bound timeout to prevent indefinite UI hanging
            socket.connect(InetSocketAddress(ipAddress, port), timeoutMs)
            socket.soTimeout = timeoutMs

            val outStream: OutputStream = socket.getOutputStream()
            val inStream: InputStream = socket.getInputStream()

            // 1. Dispatch custom payload
            val rawBytes = payload.replace("\\r", "\r").replace("\\n", "\n").toByteArray(StandardCharsets.UTF_8)
            outStream.write(rawBytes)
            outStream.flush()

            // 2. Read immediate response
            val buffer = ByteArray(4096)
            val bytesRead = inStream.read(buffer)

            if (bytesRead > 0) {
                return@withContext String(buffer, 0, bytesRead, StandardCharsets.UTF_8).trim()
            }
            
            return@withContext "[Connection Closed by Remote Host - No Response]"
            
        } catch (e: Exception) {
            return@withContext "[Error: ${e.message}]"
        } finally {
            try { socket?.close() } catch (e: Exception) {}
        }
    }
}
