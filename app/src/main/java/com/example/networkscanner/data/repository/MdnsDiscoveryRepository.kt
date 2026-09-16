package com.example.networkscanner.data.repository

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class MdnsService(
    val serviceName: String,
    val hostIp: String,
    val port: Int,
    val type: String
)

class MdnsDiscoveryRepository(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun discoverServices(serviceType: String): Flow<MdnsService> = callbackFlow {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close(Exception("Discovery failed for $serviceType with code $errorCode"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                close(Exception("Stop discovery failed with code $errorCode"))
            }

            override fun onDiscoveryStarted(serviceType: String) {}

            override fun onDiscoveryStopped(serviceType: String) {
                close()
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                // Must resolve to get the host IP address mapping
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        // Ignore individual resolve failures
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host?.hostAddress
                        if (host != null) {
                            trySend(
                                MdnsService(
                                    serviceName = serviceInfo.serviceName,
                                    hostIp = host,
                                    port = serviceInfo.port,
                                    type = serviceInfo.serviceType
                                )
                            )
                        }
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
        }

        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
