package com.example.networkscanner.domain.service

import com.example.networkscanner.domain.model.TlsAuditResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class TlsCertificateAuditor {

    // A trust manager that blindly trusts all certificates so we can inspect them
    // without the OS rejecting self-signed or expired chains during the handshake.
    private val blindTrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    suspend fun auditHost(ipAddress: String, port: Int = 443): TlsAuditResult? = withContext(Dispatchers.IO) {
        var socket: SSLSocket? = null
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(blindTrustManager), java.security.SecureRandom())
            
            val factory: SSLSocketFactory = sslContext.socketFactory
            socket = factory.createSocket() as SSLSocket
            
            // Connect with strict timeout to prevent blocking sweeps on non-responsive ports
            socket.connect(InetSocketAddress(ipAddress, port), 1000)
            socket.soTimeout = 1000
            
            socket.startHandshake()
            
            val session = socket.session
            val certificates = session.peerCertificates
            
            if (certificates.isNotEmpty() && certificates[0] is X509Certificate) {
                val leafCert = certificates[0] as X509Certificate
                return@withContext analyzeCertificate(leafCert, session.protocol, session.cipherSuite)
            }
        } catch (e: Exception) {
            // Handshake failed or not an SSL/TLS endpoint
        } finally {
            try { socket?.close() } catch (e: Exception) {}
        }
        return@withContext null
    }

    private fun analyzeCertificate(
        cert: X509Certificate, 
        protocol: String, 
        cipherSuite: String
    ): TlsAuditResult {
        val warnings = mutableListOf<String>()
        val now = Date()
        
        // 1. Expiration Checks
        val isExpired = now.after(cert.notAfter)
        if (isExpired) warnings.add("CRITICAL: Certificate is EXPIRED.")
        
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val isExpiringSoon = cert.notAfter.time - now.time < thirtyDaysMs && !isExpired
        if (isExpiringSoon) warnings.add("HIGH: Certificate expires in less than 30 days.")

        // 2. Signature Algorithm Checks
        val sigAlg = cert.sigAlgName.uppercase()
        val hasObsoleteSignature = sigAlg.contains("MD5") || sigAlg.contains("SHA1")
        if (hasObsoleteSignature) warnings.add("CRITICAL: Obsolete signature algorithm ($sigAlg) detected.")

        // 3. Self-Signed Detection
        val isSelfSigned = cert.subjectDN.name == cert.issuerDN.name
        if (isSelfSigned) warnings.add("HIGH: Certificate is Self-Signed. Traffic is vulnerable to MitM if trust is not verified out-of-band.")

        // 4. Transport Protocol
        if (protocol == "TLSv1" || protocol == "TLSv1.1" || protocol.contains("SSL")) {
            warnings.add("CRITICAL: Obsolete Protocol ($protocol) negotiated. Upgrade immediately to TLS 1.2+.")
        }

        return TlsAuditResult(
            subjectDn = cert.subjectDN.name,
            issuerDn = cert.issuerDN.name,
            protocol = protocol,
            cipherSuite = cipherSuite,
            isExpired = isExpired,
            isExpiringSoon = isExpiringSoon,
            hasObsoleteSignature = hasObsoleteSignature,
            isSelfSigned = isSelfSigned,
            warnings = warnings
        )
    }
}
